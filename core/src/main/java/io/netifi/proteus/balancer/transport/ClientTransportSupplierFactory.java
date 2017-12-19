package io.netifi.proteus.balancer.transport;

import io.netifi.proteus.discovery.DiscoveryEvent;
import io.netifi.proteus.discovery.SocketAddressFactory;
import io.rsocket.Closeable;
import io.rsocket.transport.ClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Factory that produces {@link WeighedClientTransportSupplier} implementations. Uses a Flux of
 * {@link io.netifi.proteus.discovery.DiscoveryEvent} to determine which {@link
 * WeighedClientTransportSupplier} to provide. Selects the provides based on their weighted score.
 *
 * @see io.netifi.proteus.discovery.DiscoveryEvent
 * @see WeighedClientTransportSupplier
 */
public class ClientTransportSupplierFactory
    implements Closeable, Supplier<Mono<WeighedClientTransportSupplier>> {
  private static final Logger logger =
      LoggerFactory.getLogger(ClientTransportSupplierFactory.class);
  private final Disposable subscribe;
  private final MonoProcessor<Void> onClose;
  private final List<WeighedClientTransportSupplier> suppliers;
  private final Function<SocketAddress, Supplier<ClientTransport>> factory;
  private final int minHostsAtStartup;
  private final long minHostsAtStartupTimeout;
  private MonoProcessor<Void> onMinSuppliersPresent;
  private boolean missed = false;
  private boolean minTimeout;

  public ClientTransportSupplierFactory(
      SocketAddressFactory socketAddressFactory,
      Function<SocketAddress, Supplier<ClientTransport>> factory,
      int minHostsAtStartup,
      long minHostsAtStartupTimeout) {
    resetSuppliersPresent();
    this.minHostsAtStartup = minHostsAtStartup;
    this.minHostsAtStartupTimeout = minHostsAtStartupTimeout;
    this.onClose = MonoProcessor.create();
    this.factory = factory;
    this.suppliers = new ArrayList<>();
    this.subscribe =
        socketAddressFactory
            .get()
            .doOnNext(this::handelEvent)
            .doOnError(t -> logger.error(t.getMessage(), t))
            .retry()
            .subscribe();
  }

  private synchronized void resetSuppliersPresent() {
    if (onMinSuppliersPresent == null || onMinSuppliersPresent.isTerminated()) {
      minTimeout = false;
      onMinSuppliersPresent = MonoProcessor.create();
    }
  }

  private synchronized Mono<Void> onMinSuppliersPresent() {
    Disposable subscribe = null;

    if (!minTimeout) {
      minTimeout = true;
      subscribe =
          Mono.delay(Duration.ofSeconds(minHostsAtStartupTimeout))
              .doOnNext(
                  l -> {
                    boolean empty;
                    synchronized (ClientTransportSupplierFactory.this) {
                      empty = suppliers.isEmpty();
                    }

                    if (!empty) {
                      logger.debug(
                          "min hosts at startup timeout fired - signaling suppliers present");
                      onMinSuppliersPresent.onComplete();
                    }
                  })
              .subscribe();
    }

    Disposable d = subscribe;
    return onMinSuppliersPresent.doFinally(
        s -> {
          if (d != null && !d.isDisposed()) {
            d.dispose();
          }
        });
  }

  private void handelEvent(DiscoveryEvent event) {
    synchronized (this) {
      missed = true;
    }

    int size = -1;
    SocketAddress address = event.getAddress();
    String routerId = event.getId();
    if (event.getType() == DiscoveryEvent.DiscoveryEventType.Add) {
      logger.debug(
          "adding client supplier for socket address {} with router id {}", address, routerId);
      WeighedClientTransportSupplier supplier =
          new WeighedClientTransportSupplier(routerId, factory.apply(address), address);
      synchronized (this) {
        Optional<WeighedClientTransportSupplier> first =
            suppliers
                .stream()
                .filter(
                    s -> s.getSocketAddress().equals(address) || s.getRouterId().equals(routerId))
                .findFirst();
        if (!first.isPresent()) {
          suppliers.add(supplier);
          size = suppliers.size();
        }
      }
    } else {
      logger.debug(
          "remove client supplier for socket address {} with router id {}", address, routerId);
      List<WeighedClientTransportSupplier> removedSuppliers = new ArrayList<>();
      synchronized (this) {
        ListIterator<WeighedClientTransportSupplier> iterator = suppliers.listIterator();
        while (iterator.hasNext()) {
          WeighedClientTransportSupplier s = iterator.next();
          if (s.getSocketAddress().equals(address) || s.getRouterId().equals(routerId)) {
            iterator.remove();
            removedSuppliers.add(s);
          }
        }

        size = suppliers.size();
      }

      if (!removedSuppliers.isEmpty()) {
        Flux.fromIterable(removedSuppliers)
            .flatMap(r -> r.onClose().onErrorResume(t -> Mono.empty()))
            .subscribe();
      }
    }

    if (size == 0) {
      logger.debug("no client transport suppliers present, reset to wait for suppliers");
      resetSuppliersPresent();
    } else if (size >= minHostsAtStartup) {
      if (!onMinSuppliersPresent.isTerminated()) {
        logger.debug(
            "notifying there are {} or client transport suppliers present - found {}",
            minHostsAtStartup,
            size);
        onMinSuppliersPresent.onComplete();
      }
    }
  }

  public Mono<WeighedClientTransportSupplier> get() {
    return onMinSuppliersPresent().then(Mono.fromSupplier(this::select));
  }

  private WeighedClientTransportSupplier select() {
    WeighedClientTransportSupplier supplier;

    for (; ; ) {
      List<WeighedClientTransportSupplier> _s;
      synchronized (this) {
        missed = false;
        _s = suppliers;
      }

      int size = _s.size();
      if (size == 1) {
        supplier = _s.get(0);
      } else {
        WeighedClientTransportSupplier supplier1 = null;
        WeighedClientTransportSupplier supplier2 = null;

        Random rnd = ThreadLocalRandom.current();
        int i1 = rnd.nextInt(size);
        int i2 = rnd.nextInt(size - 1);
        if (i2 >= i1) {
          i2++;
        }

        supplier1 = _s.get(i1);
        supplier2 = _s.get(i2);

        double w1 = supplier1.weight();
        double w2 = supplier2.weight();

        supplier = w1 < w2 ? supplier2 : supplier1;
      }

      synchronized (this) {
        if (!missed) {
          break;
        }
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("selecting socket {} with weight {}", supplier.toString(), supplier.weight());
    }

    return supplier;
  }

  @Override
  public Mono<Void> close() {
    return Mono.fromRunnable(onClose::onComplete).doFinally(s -> subscribe.dispose()).then();
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }

  @Override
  public String toString() {
    return "ClientTransportSupplierFactory{"
        + "subscribe="
        + subscribe.isDisposed()
        + ", onClose="
        + onClose.isTerminated()
        + ", onMinSuppliersPresent="
        + onMinSuppliersPresent.isSuccess()
        + ", missed="
        + missed
        + '}';
  }
}
