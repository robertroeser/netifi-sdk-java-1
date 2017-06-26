package io.netifi.sdk.connection.tcp;

import io.netifi.sdk.connection.Connection;
import io.netifi.sdk.serialization.RawPayload;
import io.rsocket.RSocket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Implementation of {@link Connection} class that is backed by an TCP rSocket */
public class TcpConnection implements Connection {
  private RSocket rSocket;

  public TcpConnection(RSocket rSocket) {
    this.rSocket = rSocket;
  }

  @Override
  public Mono<Void> fireAndForget(RawPayload payload) {
    return rSocket.fireAndForget(WrapperUtil.rawPayloadToPayload(payload));
  }

  @Override
  public Mono<RawPayload> requestResponse(RawPayload payload) {
    return rSocket
        .requestResponse(WrapperUtil.rawPayloadToPayload(payload))
        .map(WrapperUtil::payloadToRawPayload);
  }

  @Override
  public Flux<RawPayload> requestStream(RawPayload payload) {
    return rSocket
        .requestStream(WrapperUtil.rawPayloadToPayload(payload))
        .map(WrapperUtil::payloadToRawPayload);
  }

  @Override
  public Flux<RawPayload> requestChannel(Publisher<RawPayload> payloads) {
    return rSocket
        .requestChannel(Flux.from(payloads).map(WrapperUtil::rawPayloadToPayload))
        .map(WrapperUtil::payloadToRawPayload);
  }

  @Override
  public Mono<Void> metadataPush(RawPayload payload) {
    return rSocket.metadataPush(WrapperUtil.rawPayloadToPayload(payload));
  }

  @Override
  public double availability() {
    return rSocket.availability();
  }

  @Override
  public Mono<Void> close() {
    return rSocket.close();
  }

  @Override
  public Mono<Void> onClose() {
    return rSocket.onClose();
  }
}
