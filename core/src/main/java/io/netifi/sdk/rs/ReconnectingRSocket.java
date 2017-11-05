package io.netifi.sdk.rs;

import io.netifi.sdk.Netifi;
import io.netifi.sdk.auth.SessionUtil;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.ClientTransport;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;

public class ReconnectingRSocket implements RSocket {
  private static final Logger logger = LoggerFactory.getLogger(Netifi.class);

  private static final RSocket EMPTY_SOCKET = new AbstractRSocket() {};

  private final SessionUtil sessionUtil = SessionUtil.instance();

  private final ReplayProcessor<Mono<RSocket>> source;
  private final MonoProcessor<Void> onClose;
  private final Supplier<Payload> setupPayloadSupplier;
  private final BooleanSupplier running;
  private final Supplier<ClientTransport> clientTransportSupplier;
  private final boolean keepalive;
  private final long tickPeriodSeconds;
  private final long ackTimeoutSeconds;
  private final int missedAcks;
  private final RSocket requestHandlingRSocket;

  private final long accessKey;
  private final byte[] accessTokenBytes;

  private MonoProcessor<RSocket> currentSink;

  private ReplayProcessor<AtomicLong> currentSessionCounter = ReplayProcessor.cacheLast();
  private ReplayProcessor<byte[]> currentSessionToken = ReplayProcessor.cacheLast();

  private double availability;

  public ReconnectingRSocket(
      RSocket requestHandlingRSocket,
      Supplier<Payload> setupPayloadSupplier,
      BooleanSupplier running,
      Supplier<ClientTransport> clientTransportSupplier,
      boolean keepalive,
      long tickPeriodSeconds,
      long ackTimeoutSeconds,
      int missedAcks,
      long accessKey,
      byte[] accessTokenBytes) {
    this.requestHandlingRSocket = requestHandlingRSocket;
    this.onClose = MonoProcessor.create();
    this.source = ReplayProcessor.cacheLast();
    this.setupPayloadSupplier = setupPayloadSupplier;
    this.clientTransportSupplier = clientTransportSupplier;
    this.running = running;
    this.keepalive = keepalive;
    this.accessKey = accessKey;
    this.accessTokenBytes = accessTokenBytes;
    this.tickPeriodSeconds = tickPeriodSeconds;
    this.ackTimeoutSeconds = ackTimeoutSeconds;
    this.missedAcks = missedAcks;

    resetMono();

    connect(1).subscribe();
  }

  public SessionUtil getSessionUtil() {
    return sessionUtil;
  }

  public Mono<AtomicLong> getCurrentSessionCounter() {
    return currentSessionCounter.next();
  }

  public Mono<byte[]> getCurrentSessionToken() {
    return currentSessionToken.next();
  }

  private Mono<RSocket> connect(int retry) {
    if (running.getAsBoolean()) {
      try {
        RSocketFactory.ClientRSocketFactory connect = RSocketFactory.connect();

        if (keepalive) {
          connect =
              connect
                  .keepAlive()
                  .keepAliveAckTimeout(Duration.ofSeconds(tickPeriodSeconds))
                  .keepAliveAckTimeout(Duration.ofSeconds(ackTimeoutSeconds))
                  .keepAliveMissedAcks(missedAcks);
        } else {
          connect
              .keepAlive()
              .keepAliveAckTimeout(Duration.ofSeconds(0))
              .keepAliveAckTimeout(Duration.ofSeconds(0))
              .keepAliveMissedAcks(missedAcks);
        }

        return connect
            .setupPayload(setupPayloadSupplier.get())
            .keepAliveAckTimeout(Duration.ofSeconds(0))
            .keepAliveTickPeriod(Duration.ofSeconds(0))
            .errorConsumer(
                throwable -> logger.error("netifi sdk recieved unhandled exception", throwable))
            .acceptor(r -> requestHandlingRSocket == null ? EMPTY_SOCKET : requestHandlingRSocket)
            .transport(clientTransportSupplier)
            .start()
            .doOnNext(
                rSocket -> {
                  availability = 1.0;
                  rSocket
                      .onClose()
                      .doFinally(
                          s -> {
                            availability = 0.0;
                            connect(1).subscribe();
                          })
                      .subscribe();
                  setRSocket(rSocket);
                })
            .onErrorResume(
                t -> {
                  logger.error(t.getMessage(), t);
                  return retryConnection(retry);
                })
            .doOnNext(
                rSocket -> {
                  onClose.doFinally(s -> rSocket.close().subscribe()).subscribe();
                });

      } catch (Throwable t) {
        return retryConnection(retry);
      }
    } else {
      return Mono.empty();
    }
  }

  private Mono<RSocket> retryConnection(int retry) {
    return Mono.delay(Duration.ofSeconds(retry)).then(connect(retry < 10 ? retry + 1 : 10));
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

  @Override
  public double availability() {
    return availability;
  }

  @Override
  public Mono<Void> close() {
    return Mono.fromRunnable(onClose::onComplete)
        .doFinally(
            s -> {
              source.onComplete();
            })
        .then();
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
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
    byte[] sessionToken;
    synchronized (this) {
      long count = sessionUtil.getThirtySecondsStepsFromEpoch();
      currentSessionCounter.onNext(new AtomicLong(count));
      ByteBuffer allocate = ByteBuffer.allocate(8);
      allocate.putLong(accessKey);
      allocate.flip();
      sessionToken = sessionUtil.generateSessionToken(accessTokenBytes, allocate, count);
    }

    currentSessionToken.onNext(sessionToken);
    currentSink.onNext(rSocket);
    currentSink.onComplete();

    rSocket.onClose().doFinally(s -> resetMono()).subscribe();
  }
}
