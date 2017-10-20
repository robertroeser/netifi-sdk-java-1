package io.netifi.sdk.rs;

import io.netifi.auth.SessionUtil;
import io.netifi.nrqp.frames.RouteDestinationFlyweight;
import io.netifi.nrqp.frames.RouteType;
import io.netifi.nrqp.frames.RoutingFlyweight;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.util.PayloadImpl;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.nio.ByteBuffer;

public class DefaultNetifiSocket implements NetifiSocket {
  private final ByteBuf route;
  private final SessionUtil sessionUtil = SessionUtil.instance();
  private final MonoProcessor<Void> onClose;
  private long accessKey;
  private String fromDestination;
  private TimebasedIdGenerator generator;
  private ReconnectingRSocket reconnectingRSocket;

  public DefaultNetifiSocket(
      ReconnectingRSocket reconnectingRSocket,
      long accessKey,
      long accountId,
      String fromDestination,
      String destination,
      String group,
      byte[] accessTokenBytes,
      boolean keepalive,
      TimebasedIdGenerator generator) {
    this.accessKey = accessKey;
    this.fromDestination = fromDestination;
    this.generator = generator;
    this.onClose = MonoProcessor.create();

    reconnectingRSocket.onClose().doFinally(s -> onClose.onComplete()).subscribe();

    if (fromDestination != null && !fromDestination.equals("")) {
      int length =
          RouteDestinationFlyweight.computeLength(
              RouteType.STREAM_ID_ROUTE, fromDestination, group);
      route = Unpooled.directBuffer(length);
      RouteDestinationFlyweight.encodeRouteByDestination(
          route, RouteType.STREAM_ID_ROUTE, accountId, destination, group);
    } else {
      int length = RouteDestinationFlyweight.computeLength(RouteType.STREAM_GROUP_ROUTE, group);
      route = Unpooled.directBuffer(length);
      RouteDestinationFlyweight.encodeRouteByGroup(
          route, RouteType.STREAM_GROUP_ROUTE, accountId, group);
    }
  }

  @Override
  public double availability() {
    return 0;
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    try {
      ByteBuf unwrappedMetadata = Unpooled.wrappedBuffer(payload.getMetadata());

      int length = RoutingFlyweight.computeLength(true, fromDestination, route);

      long count = reconnectingRSocket.getCurrentSessionCounter().get().incrementAndGet();

      int requestToken =
          sessionUtil.generateRequestToken(
              reconnectingRSocket.getCurrentSessionToken().get(), payload.getData(), count);
      ByteBuffer byteBuffer = ByteBuffer.allocate(length);
      ByteBuf metadata = Unpooled.wrappedBuffer(byteBuffer);
      RoutingFlyweight.encode(
          metadata,
          true,
          requestToken,
          accessKey,
          fromDestination,
          generator.nextId(),
          route,
          unwrappedMetadata);

      return reconnectingRSocket.fireAndForget(new PayloadImpl(payload.getData(), byteBuffer));
    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    try {
      ByteBuf unwrappedMetadata = Unpooled.wrappedBuffer(payload.getMetadata());

      int length = RoutingFlyweight.computeLength(true, fromDestination, route);

      long count = reconnectingRSocket.getCurrentSessionCounter().get().incrementAndGet();

      int requestToken =
          sessionUtil.generateRequestToken(
              reconnectingRSocket.getCurrentSessionToken().get(), payload.getData(), count);
      ByteBuffer byteBuffer = ByteBuffer.allocate(length);
      ByteBuf metadata = Unpooled.wrappedBuffer(byteBuffer);
      RoutingFlyweight.encode(
          metadata,
          true,
          requestToken,
          accessKey,
          fromDestination,
          generator.nextId(),
          route,
          unwrappedMetadata);

      return reconnectingRSocket.requestResponse(new PayloadImpl(payload.getData(), byteBuffer));
    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    try {
      ByteBuf unwrappedMetadata = Unpooled.wrappedBuffer(payload.getMetadata());

      int length = RoutingFlyweight.computeLength(true, fromDestination, route);

      long count = reconnectingRSocket.getCurrentSessionCounter().get().incrementAndGet();

      int requestToken =
          sessionUtil.generateRequestToken(
              reconnectingRSocket.getCurrentSessionToken().get(), payload.getData(), count);
      ByteBuffer byteBuffer = ByteBuffer.allocate(length);
      ByteBuf metadata = Unpooled.wrappedBuffer(byteBuffer);
      RoutingFlyweight.encode(
          metadata,
          true,
          requestToken,
          accessKey,
          fromDestination,
          generator.nextId(),
          route,
          unwrappedMetadata);

      return reconnectingRSocket.requestStream(new PayloadImpl(payload.getData(), byteBuffer));
    } catch (Throwable t) {
      return Flux.error(t);
    }
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    Flux<Payload> mappedPayloads =
        Flux.from(payloads)
            .map(
                payload -> {
                  ByteBuf unwrappedMetadata = Unpooled.wrappedBuffer(payload.getMetadata());

                  int length = RoutingFlyweight.computeLength(true, fromDestination, route);

                  long count =
                      reconnectingRSocket.getCurrentSessionCounter().get().incrementAndGet();

                  int requestToken =
                      sessionUtil.generateRequestToken(
                          reconnectingRSocket.getCurrentSessionToken().get(),
                          payload.getData(),
                          count);
                  ByteBuffer byteBuffer = ByteBuffer.allocate(length);
                  ByteBuf metadata = Unpooled.wrappedBuffer(byteBuffer);
                  RoutingFlyweight.encode(
                      metadata,
                      true,
                      requestToken,
                      accessKey,
                      fromDestination,
                      generator.nextId(),
                      route,
                      unwrappedMetadata);

                  return new PayloadImpl(payload.getData(), byteBuffer);
                });

    return reconnectingRSocket.requestChannel(mappedPayloads);
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    try {
      ByteBuf unwrappedMetadata = Unpooled.wrappedBuffer(payload.getMetadata());

      int length = RoutingFlyweight.computeLength(true, fromDestination, route);

      long count = reconnectingRSocket.getCurrentSessionCounter().get().incrementAndGet();

      int requestToken =
          sessionUtil.generateRequestToken(
              reconnectingRSocket.getCurrentSessionToken().get(), payload.getData(), count);
      ByteBuffer byteBuffer = ByteBuffer.allocate(length);
      ByteBuf metadata = Unpooled.wrappedBuffer(byteBuffer);
      RoutingFlyweight.encode(
          metadata,
          true,
          requestToken,
          accessKey,
          fromDestination,
          generator.nextId(),
          route,
          unwrappedMetadata);

      return reconnectingRSocket.metadataPush(new PayloadImpl(payload.getData(), byteBuffer));
    } catch (Throwable t) {
      return Mono.error(t);
    }
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
