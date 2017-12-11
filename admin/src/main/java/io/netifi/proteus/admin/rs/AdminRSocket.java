package io.netifi.proteus.admin.rs;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.function.Function;

public class AdminRSocket implements RSocket {
  private static final Logger logger = LoggerFactory.getLogger(AdminRSocket.class);

  private final MonoProcessor<Void> onClose = MonoProcessor.create();
  private final SocketAddress address;
  private final Function<SocketAddress, Mono<RSocket>> rSocketFactory;
  private final ReplayProcessor<Mono<RSocket>> source;
  private MonoProcessor<RSocket> currentSink;

  public AdminRSocket(
      SocketAddress address, Function<SocketAddress, Mono<RSocket>> rSocketFactory) {
    this.address = address;
    this.rSocketFactory = rSocketFactory;
    this.source = ReplayProcessor.cacheLast();
    
    resetMono();
    
    connect(1).subscribe();
  }

  private Mono<RSocket> connect(int retry) {
    if (onClose.isTerminated()) {
      return Mono.empty();
    }

    return rSocketFactory
        .apply(address)
        .doOnNext(this::setRSocket)
        .onErrorResume(
            t -> {
              logger.debug(t.getMessage(), t);
              return retryConnection(retry);
            });
  }

  private Mono<RSocket> retryConnection(int retry) {
    logger.debug("delaying retry {} seconds", retry);
    return Mono.delay(Duration.ofSeconds(retry))
        .then(Mono.defer(() -> connect(Math.min(retry + 1, 10))));
  }

  public SocketAddress getSocketAddress() {
    return address;
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    return getRSocket().flatMap(rSocket -> rSocket.fireAndForget(payload));
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    return getRSocket().flatMap(rSocket -> rSocket.requestResponse(payload));
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    return getRSocket().flatMapMany(rSocket -> rSocket.requestStream(payload));
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return getRSocket().flatMapMany(rSocket -> rSocket.requestChannel(payloads));
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    return getRSocket().flatMap(rSocket -> rSocket.metadataPush(payload));
  }

  private void resetMono() {
    MonoProcessor<RSocket> _m;
    synchronized (this) {
      _m = MonoProcessor.create();
      currentSink = _m;
    }

    source.onNext(_m);
  }

  private Mono<RSocket> getRSocket() {
    return source.next().flatMap(Function.identity());
  }

  private void setRSocket(RSocket rSocket) {
    MonoProcessor<RSocket> _m;
    synchronized (this) {
      _m = currentSink;
    }

    _m.onNext(rSocket);
    _m.onComplete();

    Disposable subscribe = onClose.doFinally(s -> rSocket.close().subscribe()).subscribe();

    rSocket
        .onClose()
        .doFinally(
            s -> {
              connect(1).subscribe();
              subscribe.dispose();
              resetMono();
            })
        .subscribe();
  }

  @Override
  public Mono<Void> close() {
    return Mono.fromRunnable(onClose::onComplete).doFinally(s -> source.next()).then();
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }
}
