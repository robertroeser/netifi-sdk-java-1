package io.netifi.sdk;

import io.netifi.nrqp.frames.RouteDestinationFlyweight;
import io.netifi.nrqp.frames.RouteType;
import io.netifi.nrqp.frames.RoutingFlyweight;
import io.netifi.sdk.annotations.FIRE_FORGET;
import io.netifi.sdk.annotations.REQUEST_CHANNEL;
import io.netifi.sdk.annotations.REQUEST_RESPONSE;
import io.netifi.sdk.annotations.REQUEST_STREAM;
import io.netifi.sdk.serializer.Serializer;
import io.netifi.sdk.util.GroupUtil;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.reactivex.Flowable;
import io.reactivex.processors.ReplayProcessor;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.PayloadImpl;
import net.openhft.hashing.LongHashFunction;
import org.reactivestreams.Publisher;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
/** */
class NetifiInvocationHandler implements InvocationHandler {
  private static final LongHashFunction xx = LongHashFunction.xx();

  private final ReplayProcessor<RSocket> rSocketPublishProcessor;

  private final long accountId;

  private final String group;

  private final long destination;

  private TimebasedIdGenerator generator;

  public NetifiInvocationHandler(
      ReplayProcessor<RSocket> rSocketPublishProcessor,
      long accountId,
      String group,
      long destination,
      TimebasedIdGenerator generator) {
    this.rSocketPublishProcessor = rSocketPublishProcessor;
    this.accountId = accountId;
    this.group = group;
    this.destination = destination;
    this.generator = generator;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getDeclaringClass() == Object.class) {
      return method.invoke(this, args);
    }

    Class<?> declaringClass = method.getDeclaringClass();
    long namespaceId = xx.hashChars(declaringClass.getPackage().getName());
    long classId = xx.hashChars(declaringClass.getName());
    long methodId = xx.hashChars(method.getName());
    Type type = method.getGenericReturnType();
    ParameterizedType parameterizedType = (ParameterizedType) type;
    Type[] typeArguments = parameterizedType.getActualTypeArguments();
    Class<?> returnType = (Class<?>) typeArguments[0];

    if (!method.getReturnType().isAssignableFrom(Flowable.class)) {
      throw new IllegalStateException("method must return " + Flowable.class.getCanonicalName());
    }

    Annotation[] annotations = method.getDeclaredAnnotations();
    for (Annotation annotation : annotations) {
      if (annotation instanceof FIRE_FORGET) {
        long[] groupIds = GroupUtil.toGroupIdArray(group);
        FIRE_FORGET fire_forget = (FIRE_FORGET) annotation;
        Class<? extends Serializer> serializer =
            (Class<? extends Serializer>)
                Class.forName(
                    fire_forget.serializer(), true, Thread.currentThread().getContextClassLoader());
        Constructor<? extends Serializer> serializerConstructor =
            serializer.getDeclaredConstructor(Class.class);
        Object arg = args[0];
        Serializer<?> requestSerializer = serializerConstructor.newInstance(arg.getClass());

        return rSocketPublishProcessor.flatMap(
            rSocket -> {
              int length =
                  RouteDestinationFlyweight.computeLength(
                      RouteType.STREAM_ID_ROUTE, groupIds.length);
              ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(length);
              if (destination > 0) {
                RouteDestinationFlyweight.encodeRouteByDestination(
                    byteBuf, RouteType.STREAM_ID_ROUTE, accountId, destination, groupIds);
              } else {
                RouteDestinationFlyweight.encodeRouteByGroup(
                    byteBuf, RouteType.STREAM_GROUP_ROUTE, accountId, groupIds);
              }

              ByteBuffer byteBuffer = ByteBuffer.allocateDirect(byteBuf.capacity());
              byteBuf.getBytes(0, byteBuffer);

              PayloadImpl payload = new PayloadImpl(requestSerializer.serialize(arg), byteBuffer);

              return rSocket.fireAndForget(payload);
            });
      } else if (annotation instanceof REQUEST_CHANNEL) {
        long[] groupIds = GroupUtil.toGroupIdArray(group);
        REQUEST_CHANNEL request_channel = (REQUEST_CHANNEL) annotation;
        Class<? extends Serializer> serializer =
            (Class<? extends Serializer>)
                Class.forName(
                    request_channel.serializer(),
                    true,
                    Thread.currentThread().getContextClassLoader());
        Constructor<? extends Serializer> serializerConstructor =
            serializer.getDeclaredConstructor(Class.class);
        Object arg = args[0];
        Serializer<?> requestSerializer = serializerConstructor.newInstance(arg.getClass());
        Serializer<?> responseSerializer = serializerConstructor.newInstance((returnType));

        return rSocketPublishProcessor.flatMap(
            rSocket -> {
              Flowable<Payload> map =
                  Flowable.fromPublisher((Publisher) arg)
                      .map(
                          o -> {
                            ByteBuffer data = requestSerializer.serialize(o);
                            int length =
                                RouteDestinationFlyweight.computeLength(
                                    RouteType.STREAM_ID_ROUTE, groupIds.length);
                            ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                            if (destination > 0) {
                              RouteDestinationFlyweight.encodeRouteByDestination(
                                  byteBuf,
                                  RouteType.STREAM_ID_ROUTE,
                                  accountId,
                                  destination,
                                  groupIds);
                            } else {
                              RouteDestinationFlyweight.encodeRouteByGroup(
                                  byteBuf, RouteType.STREAM_GROUP_ROUTE, accountId, groupIds);
                            }

                            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(byteBuf.capacity());
                            byteBuf.getBytes(0, byteBuffer);

                            PayloadImpl payload =
                                new PayloadImpl(requestSerializer.serialize(arg), byteBuffer);

                            return payload;
                          });

              return rSocket
                  .requestChannel(map)
                  .map(
                      payload -> {
                        ByteBuffer data = payload.getData();
                        return responseSerializer.deserialize(data);
                      });
            });

      } else if (annotation instanceof REQUEST_RESPONSE) {
        long[] groupIds = GroupUtil.toGroupIdArray(group);
        REQUEST_RESPONSE request_response = (REQUEST_RESPONSE) annotation;
        Class<? extends Serializer> serializer =
            (Class<? extends Serializer>)
                Class.forName(
                    request_response.serializer(),
                    true,
                    Thread.currentThread().getContextClassLoader());
        Constructor<? extends Serializer> serializerConstructor =
            serializer.getDeclaredConstructor(Class.class);
        Object arg = args != null ? args[0] : null;
        Serializer<?> requestSerializer =
            arg != null ? serializerConstructor.newInstance(arg.getClass()) : null;
        Serializer<?> responseSerializer = serializerConstructor.newInstance((returnType));

        return rSocketPublishProcessor.flatMap(
            rSocket -> {
              ByteBuf route;

              if (destination > 0) {
                int length =
                    RouteDestinationFlyweight.computeLength(
                        RouteType.STREAM_ID_ROUTE, groupIds.length);
                route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                RouteDestinationFlyweight.encodeRouteByDestination(
                    route, RouteType.STREAM_ID_ROUTE, accountId, destination, groupIds);
              } else {
                int length =
                    RouteDestinationFlyweight.computeLength(
                        RouteType.STREAM_GROUP_ROUTE, groupIds.length);
                route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                RouteDestinationFlyweight.encodeRouteByGroup(
                    route, RouteType.STREAM_GROUP_ROUTE, accountId, groupIds);
              }

              int length = RoutingFlyweight.computeLength(true, false, false, route);

              ByteBuf metadata = PooledByteBufAllocator.DEFAULT.directBuffer(length);
              RoutingFlyweight.encode(
                  metadata,
                  true,
                  false,
                  false,
                  0,
                  0,
                  destination,
                  0,
                  namespaceId,
                  classId,
                  methodId,
                  generator.nextId(),
                  route);

              ByteBuffer data =
                  arg != null ? requestSerializer.serialize(arg) : Frame.NULL_BYTEBUFFER;
              byte[] bytes = new byte[metadata.capacity()];
              metadata.getBytes(0, bytes);
              PayloadImpl payload = new PayloadImpl(data, ByteBuffer.wrap(bytes));

              return rSocket
                  .requestResponse(payload)
                  .map(
                      payload1 -> {
                        ByteBuffer data1 = payload1.getData();
                        return responseSerializer.deserialize(data1);
                      });
            });

      } else if (annotation instanceof REQUEST_STREAM) {
          long[] groupIds = GroupUtil.toGroupIdArray(group);
          REQUEST_STREAM request_stream = (REQUEST_STREAM) annotation;
          Class<? extends Serializer> serializer =
              (Class<? extends Serializer>)
                  Class.forName(
                      request_stream.serializer(),
                      true,
                      Thread.currentThread().getContextClassLoader());
          Constructor<? extends Serializer> serializerConstructor =
              serializer.getDeclaredConstructor(Class.class);
          Object arg = args != null ? args[0] : null;
          Serializer<?> requestSerializer =
              arg != null ? serializerConstructor.newInstance(arg.getClass()) : null;
          Serializer<?> responseSerializer = serializerConstructor.newInstance((returnType));
    
          return rSocketPublishProcessor.flatMap(
              rSocket -> {
                  ByteBuf route;
            
                  if (destination > 0) {
                      int length =
                          RouteDestinationFlyweight.computeLength(
                              RouteType.STREAM_ID_ROUTE, groupIds.length);
                      route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                      RouteDestinationFlyweight.encodeRouteByDestination(
                          route, RouteType.STREAM_ID_ROUTE, accountId, destination, groupIds);
                  } else {
                      int length =
                          RouteDestinationFlyweight.computeLength(
                              RouteType.STREAM_GROUP_ROUTE, groupIds.length);
                      route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                      RouteDestinationFlyweight.encodeRouteByGroup(
                          route, RouteType.STREAM_GROUP_ROUTE, accountId, groupIds);
                  }
            
                  int length = RoutingFlyweight.computeLength(true, false, false, route);
            
                  ByteBuf metadata = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                  RoutingFlyweight.encode(
                      metadata,
                      true,
                      false,
                      false,
                      0,
                      0,
                      destination,
                      0,
                      namespaceId,
                      classId,
                      methodId,
                      generator.nextId(),
                      route);
            
                  ByteBuffer data =
                      arg != null ? requestSerializer.serialize(arg) : Frame.NULL_BYTEBUFFER;
                  byte[] bytes = new byte[metadata.capacity()];
                  metadata.getBytes(0, bytes);
                  PayloadImpl payload = new PayloadImpl(data, ByteBuffer.wrap(bytes));
            
                  return rSocket
                             .requestStream(payload)
                             .map(
                                 payload1 -> {
                                     ByteBuffer data1 = payload1.getData();
                                     return responseSerializer.deserialize(data1);
                                 });
              });
      }
    }

    throw new IllegalStateException("no method found with netifi annotation");
  }
}
