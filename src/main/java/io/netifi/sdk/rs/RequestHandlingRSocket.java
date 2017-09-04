package io.netifi.sdk.rs;

import io.netifi.nrqp.frames.RoutingFlyweight;
import io.netifi.sdk.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.Flowable;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.util.PayloadImpl;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/** */
public class RequestHandlingRSocket extends AbstractRSocket {
  private ConcurrentHashMap<String, RequestHandlerMetadata> cachedMethods =
      new ConcurrentHashMap<>();

  public RequestHandlingRSocket(ConcurrentHashMap<String, RequestHandlerMetadata> cachedMethods) {
    this.cachedMethods = cachedMethods;
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    try {
      ByteBuf byteBuf = Unpooled.wrappedBuffer(payload.getMetadata());
      long namespaceId = RoutingFlyweight.namespaceId(byteBuf);
      long classId = RoutingFlyweight.classId(byteBuf);
      long methodId = RoutingFlyweight.methodId(byteBuf);

      String key = namespaceId + ":" + classId + ":" + methodId;

      RequestHandlerMetadata metadata = cachedMethods.get(key);

      if (metadata == null) {
        return Mono.error(new IllegalStateException("no request handle found for " + key));
      }

      Method method = metadata.getMethod();
      Object object = metadata.getObject();
      Serializer<?> requestSerializer = metadata.getRequestSerializer();

      ByteBuffer data = payload.getData();
      Object apply = requestSerializer != null ? requestSerializer.deserialize(data) : null;

      Flowable<Void> single =
          (Flowable<Void>) (apply != null ? method.invoke(object, apply) : method.invoke(object));

      return Mono.from(single);
    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    try {
      ByteBuf byteBuf = Unpooled.wrappedBuffer(payload.getMetadata());
      long namespaceId = RoutingFlyweight.namespaceId(byteBuf);
      long classId = RoutingFlyweight.classId(byteBuf);
      long methodId = RoutingFlyweight.methodId(byteBuf);

      String key = namespaceId + ":" + classId + ":" + methodId;

      RequestHandlerMetadata metadata = cachedMethods.get(key);

      if (metadata == null) {
        return Mono.error(new IllegalStateException("no request handle found for " + key));
      }

      Method method = metadata.getMethod();
      Object object = metadata.getObject();
      Serializer<?> requestSerializer = metadata.getRequestSerializer();

      ByteBuffer data = payload.getData();
      Object apply = requestSerializer != null ? requestSerializer.deserialize(data) : null;

      Flowable<PayloadImpl> map =
          ((Flowable<Object>)
                  (apply != null ? method.invoke(object, apply) : method.invoke(object)))
              .map(
                  o -> {
                    Serializer<?> responseSerializer = metadata.getResponseSerializer();
                    ByteBuffer serialize = responseSerializer.serialize(o);
                    return new PayloadImpl(serialize);
                  });

      return Mono.from(map);
    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    try {
      ByteBuf byteBuf = Unpooled.wrappedBuffer(payload.getMetadata());
      long namespaceId = RoutingFlyweight.namespaceId(byteBuf);
      long classId = RoutingFlyweight.classId(byteBuf);
      long methodId = RoutingFlyweight.methodId(byteBuf);

      String key = namespaceId + ":" + classId + ":" + methodId;

      RequestHandlerMetadata metadata = cachedMethods.get(key);

      if (metadata == null) {
        return Flux.error(new IllegalStateException("no request handle found for " + key));
      }

      Method method = metadata.getMethod();
      Object object = metadata.getObject();
      Serializer<?> requestSerializer = metadata.getRequestSerializer();

      ByteBuffer data = payload.getData();
      Object apply = requestSerializer != null ? requestSerializer.deserialize(data) : null;

      Flowable<PayloadImpl> map =
          ((Flowable<Object>)
                  (apply != null ? method.invoke(object, apply) : method.invoke(object)))
              .map(
                  o -> {
                    Serializer<?> responseSerializer = metadata.getResponseSerializer();
                    ByteBuffer serialize = responseSerializer.serialize(o);
                    return new PayloadImpl(serialize);
                  });

      return Flux.from(map);
    } catch (Throwable t) {
      return Flux.error(t);
    }
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return Flux.from(payloads)
        .groupBy(
            payload -> {
              ByteBuf byteBuf = Unpooled.wrappedBuffer(payload.getMetadata());
              long namespaceId = RoutingFlyweight.namespaceId(byteBuf);
              long classId = RoutingFlyweight.classId(byteBuf);
              long methodId = RoutingFlyweight.methodId(byteBuf);

              String key = namespaceId + ":" + classId + ":" + methodId;

              RequestHandlerMetadata metadata = cachedMethods.get(key);

              if (metadata == null) {
                throw new IllegalStateException("no request handle found for " + key);
              }

              return metadata;
            })
        .flatMap(
            groupedPayloads -> {
              try {
                RequestHandlerMetadata metadata = groupedPayloads.key();
                Method method = metadata.getMethod();
                Serializer<?> requestSerializer = metadata.getRequestSerializer();

                Flux<?> map =
                    groupedPayloads.map(
                        payload -> {
                          ByteBuffer data = payload.getData();
                          return requestSerializer.deserialize(data);
                        });

                Flowable<PayloadImpl> map1 =
                    ((Flowable<Object>) method.invoke(metadata.getObject(), map))
                        .map(
                            o -> {
                              Serializer<?> responseSerializer = metadata.getResponseSerializer();
                              ByteBuffer data = responseSerializer.serialize(o);
                              return new PayloadImpl(data);
                            });
                return Flux.from(map1);
              } catch (Throwable t) {
                return Flux.error(t);
              }
            });
  }
}
