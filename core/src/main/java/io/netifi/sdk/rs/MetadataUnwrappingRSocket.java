package io.netifi.sdk.rs;

import io.netifi.sdk.frames.RoutingFlyweight;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.RSocketProxy;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MetadataUnwrappingRSocket extends RSocketProxy {
  private MetadataUnwrappingRSocket(RSocket source) {
    super(source);
  }

  public static MetadataUnwrappingRSocket wrap(RSocket rSocket) {
    return new MetadataUnwrappingRSocket(rSocket);
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    return super.fireAndForget(new UnwrappingPayload(payload));
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    return super.requestResponse(new UnwrappingPayload(payload));
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    return super.requestStream(new UnwrappingPayload(payload));
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return super.requestChannel(Flux.from(payloads).map(UnwrappingPayload::new));
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    return super.metadataPush(new UnwrappingPayload(payload));
  }

  class UnwrappingPayload implements Payload {
    private Payload payload;

    public UnwrappingPayload(Payload payload) {
      this.payload = payload;
    }

    @Override
    public boolean hasMetadata() {
      return payload.hasMetadata();
    }

    @Override
    public ByteBuffer getMetadata() {
      ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
      ByteBuf byteBuf = RoutingFlyweight.wrappedMetadata(metadata);
      return byteBuf.nioBuffer(0, byteBuf.capacity());
    }

    @Override
    public ByteBuffer getData() {
      return payload.getData();
    }
  }
}
