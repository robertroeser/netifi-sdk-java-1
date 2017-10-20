package io.netifi.sdk.rs;

import io.netifi.auth.SessionUtil;
import io.netifi.sdk.Netifi;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.ClientTransport;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

public class ReconnectingRSocket implements RSocket {
  private static final Logger logger = LoggerFactory.getLogger(Netifi.class);

  private final SessionUtil sessionUtil = SessionUtil.instance();

  private final AtomicReference<AtomicLong> currentSessionCounter = new AtomicReference<>();
  private final AtomicReference<byte[]> currentSessionToken = new AtomicReference<>();

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

  public AtomicReference<AtomicLong> getCurrentSessionCounter() {
    return currentSessionCounter;
  }

  public AtomicReference<byte[]> getCurrentSessionToken() {
    return currentSessionToken;
  }

  private Mono<RSocket> connect(int retry) {
    if (running.getAsBoolean()) {
      try {
        RSocketFactory.ClientRSocketFactory connect = RSocketFactory.connect();

        if (keepalive) {
          connect = connect.keepAlive();
          connect
              .keepAliveAckTimeout(Duration.ofSeconds(tickPeriodSeconds))
              .keepAliveAckTimeout(Duration.ofSeconds(ackTimeoutSeconds))
              .keepAliveMissedAcks(missedAcks);
        }

        return connect
            .setupPayload(setupPayloadSupplier.get())
            .acceptor(r -> requestHandlingRSocket)
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
                  return Mono.delay(Duration.ofSeconds(retry))
                      .then(connect(retry < 10 ? retry + 1 : 10));
                });

      } catch (Throwable t) {
        return Mono.delay(Duration.ofSeconds(retry)).then(connect(retry < 10 ? retry + 1 : 10));
      }
    } else {
      return Mono.empty();
    }
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
    return Mono.fromRunnable(onClose::onComplete).doFinally(s -> source.onComplete()).then();
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
    long count = sessionUtil.getThirtySecondsStepsFromEpoch();
    currentSessionCounter.set(new AtomicLong(count));
    ByteBuffer allocate = ByteBuffer.allocate(8);
    allocate.putLong(accessKey);
    allocate.flip();
    byte[] sessionToken = sessionUtil.generateSessionToken(accessTokenBytes, allocate, count);
    currentSessionToken.set(sessionToken);

    currentSink.onNext(rSocket);
    currentSink.onComplete();

    rSocket.onClose().doFinally(s -> resetMono()).subscribe();
  }
}
