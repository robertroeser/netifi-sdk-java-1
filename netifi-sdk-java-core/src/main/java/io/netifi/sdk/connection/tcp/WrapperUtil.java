package io.netifi.sdk.connection.tcp;

import io.netifi.sdk.connection.Handler;
import io.netifi.sdk.serialization.RawPayload;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.PayloadImpl;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

/** Utils to wrap an RSocket with a Handler and a Handler with an RSocket */
public final class WrapperUtil {
  private WrapperUtil() {}

  public static Handler rsocketToHandler(RSocket rSocket) {
    return new Handler() {
      @Override
      public Mono<Void> fireAndForget(RawPayload payload) {
        return rSocket.fireAndForget(rawPayloadToPayload(payload));
      }

      @Override
      public Mono<RawPayload> requestResponse(RawPayload payload) {
        return rSocket
            .requestResponse(rawPayloadToPayload(payload))
            .map(WrapperUtil::payloadToRawPayload);
      }

      @Override
      public Flux<RawPayload> requestStream(RawPayload payload) {
        return rSocket
            .requestStream(rawPayloadToPayload(payload))
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
        return rSocket.metadataPush(rawPayloadToPayload(payload));
      }
    };
  }

  public static RSocket handlerToRSocket(Handler handler) {
    return new AbstractRSocket() {
      @Override
      public Mono<Void> fireAndForget(Payload payload) {
        return handler.fireAndForget(payloadToRawPayload(payload));
      }

      @Override
      public Mono<Payload> requestResponse(Payload payload) {
        return handler
            .requestResponse(payloadToRawPayload(payload))
            .map(WrapperUtil::rawPayloadToPayload);
      }

      @Override
      public Flux<Payload> requestStream(Payload payload) {
        return handler
            .requestStream(payloadToRawPayload(payload))
            .map(WrapperUtil::rawPayloadToPayload);
      }

      @Override
      public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return handler
            .requestChannel(Flux.from(payloads).map(WrapperUtil::payloadToRawPayload))
            .map(WrapperUtil::rawPayloadToPayload);
      }

      @Override
      public Mono<Void> metadataPush(Payload payload) {
        return handler.metadataPush(payloadToRawPayload(payload));
      }
    };
  }

  public static RawPayload payloadToRawPayload(Payload payload) {
    return new RawPayload() {
      @Override
      public ByteBuffer getMetadata() {
        return payload.getMetadata();
      }

      @Override
      public ByteBuffer getData() {
        return payload.getMetadata();
      }
    };
  }

  public static Payload rawPayloadToPayload(RawPayload rawPayload) {
    return new PayloadImpl(rawPayload.getData(), rawPayload.getMetadata());
  }
}
