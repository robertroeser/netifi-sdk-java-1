package io.netifi.proteus.balancer.transport;

import io.netifi.proteus.balancer.stats.Ewma;
import io.netifi.proteus.rs.WeightedRSocket;
import io.rsocket.Closeable;
import io.rsocket.transport.ClientTransport;
import io.rsocket.util.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Function;
import java.util.function.Supplier;

public class WeighedClientTransportSupplier
    implements Function<Flux<WeightedRSocket>, Supplier<ClientTransport>>, Closeable {

  private static final Logger logger =
      LoggerFactory.getLogger(WeighedClientTransportSupplier.class);
  private static AtomicInteger SUPPLIER_ID = new AtomicInteger();
  private final MonoProcessor<Void> onClose;
  private final String routerId;
  private final int supplierId;
  private final Supplier<ClientTransport> internal;
  private final Ewma errorPercentage;
  private final Ewma latency;
  private final SocketAddress socketAddress;
  private final AtomicInteger activeConnections;

  @SuppressWarnings("unused")
  private volatile int selectedCounter = 1;

  public WeighedClientTransportSupplier(
      String routerId, Supplier<ClientTransport> internal, SocketAddress socketAddress) {
    this.routerId = routerId;
    this.supplierId = SUPPLIER_ID.incrementAndGet();
    this.internal = internal;
    this.socketAddress = socketAddress;
    this.errorPercentage = new Ewma(5, TimeUnit.SECONDS, 1.0);
    this.latency = new Ewma(1, TimeUnit.MINUTES, Clock.unit().convert(1L, TimeUnit.SECONDS));
    this.activeConnections = new AtomicInteger();
    this.onClose = MonoProcessor.create();
  }

  @Override
  public Supplier<ClientTransport> apply(Flux<WeightedRSocket> weightedSocketFlux) {
    if (onClose.isTerminated()) {
      throw new IllegalStateException("WeightedClientTransportSupplier is closed");
    }

    Disposable subscribe =
        weightedSocketFlux
            .doOnNext(
                weightedRSocket -> {
                  if (logger.isDebugEnabled()) {
                    logger.debug("recording stats {}", weightedRSocket.toString());
                  }
                  double e = weightedRSocket.errorPercentage();
                  double i = weightedRSocket.higherQuantileLatency();

                  errorPercentage.insert(e);
                  latency.insert(i);
                })
            .subscribe();

    return () ->
        () ->
            internal
                .get()
                .connect()
                .doOnNext(
                    duplexConnection -> {
                      int i = activeConnections.incrementAndGet();
                      logger.debug(
                          "supplier id - {} - opened connection to {} - active connections {}",
                          supplierId,
                          socketAddress,
                          i);

                      Disposable onCloseDisposable =
                          onClose.then(duplexConnection.close()).subscribe();

                      duplexConnection
                          .onClose()
                          .doFinally(
                              s -> {
                                int d = activeConnections.decrementAndGet();
                                logger.debug(
                                    "supplier id - {} - closed connection {} - active connections {}",
                                    supplierId,
                                    socketAddress,
                                    d);
                                onCloseDisposable.dispose();
                                subscribe.dispose();
                              })
                          .subscribe();
                    })
                .doOnError(t -> errorPercentage.insert(0.0));
  }

  public double errorPercentage() {
    return errorPercentage.value();
  }

  public double latency() {
    return latency.value();
  }

  public int activeConnections() {
    return activeConnections.get();
  }

  public double weight() {
    double e = errorPercentage();
    double l = latency();
    int a = activeConnections();

    return e * 1.0 / (1.0 + l * (a + 1));
  }

  public SocketAddress getSocketAddress() {
    return socketAddress;
  }

  public String getRouterId() {
    return routerId;
  }

  @Override
  public Mono<Void> close() {
    return Mono.fromRunnable(onClose::onComplete);
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WeighedClientTransportSupplier supplier = (WeighedClientTransportSupplier) o;

    return socketAddress.equals(supplier.socketAddress);
  }

  @Override
  public int hashCode() {
    return socketAddress.hashCode();
  }
  
  @Override
  public String toString() {
    return "WeighedClientTransportSupplier{" +
               "routerId='" + routerId + '\'' +
               ", supplierId=" + supplierId +
               ", errorPercentage=" + errorPercentage +
               ", latency=" + latency +
               ", socketAddress=" + socketAddress +
               ", activeConnections=" + activeConnections +
               ", selectedCounter=" + selectedCounter +
               '}';
  }
}
