package io.netifi.testing.protobuf;

@javax.annotation.Generated(
  value = "by Proteus proto compiler (version 0.2.4)",
  comments = "Source: io.netifi.sdk.proteus/simpleservice.proto"
)
public final class SimpleServiceServer extends io.netifi.proteus.AbstractProteusService {
  private final SimpleService service;

  public SimpleServiceServer(SimpleService service) {
    this.service = service;
  }

  @java.lang.Override
  public int getNamespaceId() {
    return SimpleService.NAMESPACE_ID;
  }

  @java.lang.Override
  public int getServiceId() {
    return SimpleService.SERVICE_ID;
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<Void> fireAndForget(io.rsocket.Payload payload) {
    try {
      io.netty.buffer.ByteBuf metadata = payload.sliceMetadata();
      switch (io.netifi.proteus.frames.ProteusMetadata.methodId(metadata)) {
        case SimpleService.METHOD_FIRE_AND_FORGET:
          {
            com.google.protobuf.CodedInputStream is =
                com.google.protobuf.CodedInputStream.newInstance(payload.getData());
            return service.fireAndForget(io.netifi.testing.protobuf.SimpleRequest.parseFrom(is));
          }
        default:
          {
            return reactor.core.publisher.Mono.error(new UnsupportedOperationException());
          }
      }
    } catch (Throwable t) {
      return reactor.core.publisher.Mono.error(t);
    } finally {
      payload.release();
    }
  }

  @java.lang.Override
  public reactor.core.publisher.Mono<io.rsocket.Payload> requestResponse(
      io.rsocket.Payload payload) {
    try {
      io.netty.buffer.ByteBuf metadata = payload.sliceMetadata();
      switch (io.netifi.proteus.frames.ProteusMetadata.methodId(metadata)) {
        case SimpleService.METHOD_UNARY_RPC:
          {
            com.google.protobuf.CodedInputStream is =
                com.google.protobuf.CodedInputStream.newInstance(payload.getData());
            return service
                .unaryRpc(io.netifi.testing.protobuf.SimpleRequest.parseFrom(is))
                .map(serializer);
          }
        default:
          {
            return reactor.core.publisher.Mono.error(new UnsupportedOperationException());
          }
      }
    } catch (Throwable t) {
      return reactor.core.publisher.Mono.error(t);
    } finally {
      payload.release();
    }
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<io.rsocket.Payload> requestStream(io.rsocket.Payload payload) {
    try {
      io.netty.buffer.ByteBuf metadata = payload.sliceMetadata();
      switch (io.netifi.proteus.frames.ProteusMetadata.methodId(metadata)) {
        case SimpleService.METHOD_STREAM_ON_FIRE_AND_FORGET:
          {
            com.google.protobuf.CodedInputStream is =
                com.google.protobuf.CodedInputStream.newInstance(payload.getData());
            return service
                .streamOnFireAndForget(io.netifi.testing.protobuf.Empty.parseFrom(is))
                .map(serializer);
          }
        case SimpleService.METHOD_SERVER_STREAMING_RPC:
          {
            com.google.protobuf.CodedInputStream is =
                com.google.protobuf.CodedInputStream.newInstance(payload.getData());
            return service
                .serverStreamingRpc(io.netifi.testing.protobuf.SimpleRequest.parseFrom(is))
                .map(serializer);
          }
        default:
          {
            return reactor.core.publisher.Flux.error(new UnsupportedOperationException());
          }
      }
    } catch (Throwable t) {
      return reactor.core.publisher.Flux.error(t);
    } finally {
      payload.release();
    }
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<io.rsocket.Payload> requestChannel(
      io.rsocket.Payload payload, reactor.core.publisher.Flux<io.rsocket.Payload> publisher) {
    try {
      io.netty.buffer.ByteBuf metadata = payload.sliceMetadata();
      switch (io.netifi.proteus.frames.ProteusMetadata.methodId(metadata)) {
        case SimpleService.METHOD_CLIENT_STREAMING_RPC:
          {
            reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleRequest> messages =
                publisher.map(deserializer(io.netifi.testing.protobuf.SimpleRequest.parser()));
            return service.clientStreamingRpc(messages).map(serializer).flux();
          }
        case SimpleService.METHOD_BIDI_STREAMING_RPC:
          {
            reactor.core.publisher.Flux<io.netifi.testing.protobuf.SimpleRequest> messages =
                publisher.map(deserializer(io.netifi.testing.protobuf.SimpleRequest.parser()));
            return service.bidiStreamingRpc(messages).map(serializer);
          }
        default:
          {
            return reactor.core.publisher.Flux.error(new UnsupportedOperationException());
          }
      }
    } catch (Throwable t) {
      return reactor.core.publisher.Flux.error(t);
    }
  }

  @java.lang.Override
  public reactor.core.publisher.Flux<io.rsocket.Payload> requestChannel(
      org.reactivestreams.Publisher<io.rsocket.Payload> payloads) {
    return new io.rsocket.internal.SwitchTransform<io.rsocket.Payload, io.rsocket.Payload>(
        payloads,
        new java.util.function.BiFunction<
            io.rsocket.Payload, reactor.core.publisher.Flux<io.rsocket.Payload>,
            org.reactivestreams.Publisher<? extends io.rsocket.Payload>>() {
          @java.lang.Override
          public org.reactivestreams.Publisher<io.rsocket.Payload> apply(
              io.rsocket.Payload payload,
              reactor.core.publisher.Flux<io.rsocket.Payload> publisher) {
            return requestChannel(payload, publisher);
          }
        });
  }

  private static final java.util.function.Function<
          com.google.protobuf.MessageLite, io.rsocket.Payload>
      serializer =
          new java.util.function.Function<com.google.protobuf.MessageLite, io.rsocket.Payload>() {
            @java.lang.Override
            public io.rsocket.Payload apply(com.google.protobuf.MessageLite message) {
              io.netty.buffer.ByteBuf byteBuf =
                  io.netty.buffer.ByteBufAllocator.DEFAULT.directBuffer(
                      message.getSerializedSize());
              try {
                message.writeTo(
                    com.google.protobuf.CodedOutputStream.newInstance(
                        byteBuf.nioBuffer(0, byteBuf.writableBytes())));
                byteBuf.writerIndex(byteBuf.capacity());
                return io.rsocket.util.ByteBufPayload.create(byteBuf);
              } catch (Throwable t) {
                byteBuf.release();
                throw new RuntimeException(t);
              }
            }
          };

  private static <T> java.util.function.Function<io.rsocket.Payload, T> deserializer(
      final com.google.protobuf.Parser<T> parser) {
    return new java.util.function.Function<io.rsocket.Payload, T>() {
      @java.lang.Override
      public T apply(io.rsocket.Payload payload) {
        try {
          com.google.protobuf.CodedInputStream is =
              com.google.protobuf.CodedInputStream.newInstance(payload.getData());
          return parser.parseFrom(is);
        } catch (Throwable t) {
          throw new RuntimeException(t);
        } finally {
          payload.release();
        }
      }
    };
  }
}
