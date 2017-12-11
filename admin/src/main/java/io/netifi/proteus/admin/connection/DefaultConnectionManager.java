package io.netifi.proteus.admin.connection;

import io.netifi.proteus.admin.rs.AdminRSocket;
import io.netifi.proteus.connection.DiscoveryEvent;
import io.rsocket.Closeable;
import io.rsocket.RSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.net.SocketAddress;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public class DefaultConnectionManager implements ConnectionManager, Closeable {
  private static final Logger logger = LoggerFactory.getLogger(DefaultConnectionManager.class);
  private final CopyOnWriteArrayList<AdminRSocket> rSockets;
  private final Function<SocketAddress, Mono<AdminRSocket>> adminSocketFactory;
  private final MonoProcessor<Void> onClose;
  private final DirectProcessor<AdminRSocket> newSocketProcessor;

  private MonoProcessor<Void> onConnectionPresent;
  private boolean missed;

  public DefaultConnectionManager(
      Flux<DiscoveryEvent> eventFlux,
      Function<SocketAddress, Mono<AdminRSocket>> adminSocketFactory) {
    this.newSocketProcessor = DirectProcessor.create();
    this.rSockets = new CopyOnWriteArrayList<>();
    this.adminSocketFactory = adminSocketFactory;
    this.onClose = MonoProcessor.create();

    reset();

    Disposable subscribe =
        eventFlux
            .flatMap(this::handelEvent)
            .doOnNext(rSockets::add)
            .doOnNext(newSocketProcessor::onNext)
            .retry()
            .subscribe();

    onClose.doFinally(s -> subscribe.dispose()).subscribe();
  }

  private synchronized Mono<AdminRSocket> handelEvent(DiscoveryEvent event) {
    missed = true;
    Mono<AdminRSocket> mono;
    SocketAddress address = event.getAddress();
    if (event == DiscoveryEvent.Add) {
      logger.debug("adding AdminRSocket for socket address {}", address);
      mono = adminSocketFactory.apply(address);
    } else {
      logger.debug("removing AdminRSocket for socket address {}", address);
      Optional<AdminRSocket> first =
          rSockets.stream().filter(s -> s.getSocketAddress().equals(address)).findFirst();

      if (first.isPresent()) {
        AdminRSocket adminRSocket = first.get();
        rSockets.remove(adminRSocket);
        adminRSocket.close().subscribe();
      }

      mono = Mono.empty();
    }

    if (rSockets.isEmpty()) {
      logger.debug("no client transport suppliers present, reset to wait an AdminRSocket");
      reset();
    } else {
      if (!onConnectionPresent.isTerminated()) {
        logger.debug("notifying an AdminRSocket is present");
        onConnectionPresent.onComplete();
      }
    }

    return mono;
  }

  private synchronized void reset() {
    this.onConnectionPresent = MonoProcessor.create();
  }

  private synchronized Mono<Void> getOnConnectionPresent() {
    return onConnectionPresent;
  }

  private RSocket get() {
    RSocket rSocket;
    for (; ; ) {
      synchronized (this) {
        missed = false;
      }
      int size = rSockets.size();
      if (size == 1) {
        rSocket = rSockets.get(0);
      } else {
        int i = ThreadLocalRandom.current().nextInt(size);
        rSocket = rSockets.get(i);
      }

      synchronized (this) {
        if (!missed) {
          break;
        }
      }
    }

    return rSocket;
  }

  @Override
  public Mono<RSocket> getRSocket() {
    return getOnConnectionPresent().then(Mono.fromSupplier(this::get));
  }

  @Override
  public Flux<? extends RSocket> getRSockets() {
    return Flux.fromIterable(rSockets).concatWith(newSocketProcessor.onBackpressureBuffer());
  }

  @Override
  public Mono<Void> close() {
    return Mono.fromRunnable(onClose::onComplete);
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }
}
