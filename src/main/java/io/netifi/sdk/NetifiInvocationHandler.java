package io.netifi.sdk;

import io.netifi.nrqp.frames.RouteDestinationFlyweight;
import io.netifi.nrqp.frames.RouteType;
import io.netifi.nrqp.frames.RoutingFlyweight;
import io.netifi.sdk.annotations.FireForget;
import io.netifi.sdk.annotations.RequestChannel;
import io.netifi.sdk.annotations.RequestResponse;
import io.netifi.sdk.annotations.RequestStream;
import io.netifi.sdk.serializer.Serializer;
import io.netifi.sdk.serializer.Serializers;
import io.netifi.sdk.util.ClassUtil;
import io.netifi.sdk.util.GroupUtil;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.reactivex.Flowable;
import io.reactivex.processors.ReplayProcessor;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.PayloadImpl;
import org.reactivestreams.Publisher;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import static io.netifi.sdk.util.HashUtil.hash;

/** */
class NetifiInvocationHandler implements InvocationHandler {

  private final ReplayProcessor<RSocket> rSocketPublishProcessor;

  private final long fromAccountId;

  private final long[] fromGroupIds;

  private final long fromDestination;

  private final long accountId;

  private final String group;

  private final long destination;

  private TimebasedIdGenerator generator;

  public NetifiInvocationHandler(
      ReplayProcessor<RSocket> rSocketPublishProcessor,
      long accountId,
      String group,
      long destination,
      long fromAccountId,
      long[] fromGroupIds,
      long fromDestination,
      TimebasedIdGenerator generator) {
    this.rSocketPublishProcessor = rSocketPublishProcessor;
    this.accountId = accountId;
    this.group = group;
    this.destination = destination;
    this.generator = generator;
    this.fromAccountId = fromAccountId;
    this.fromGroupIds = fromGroupIds;
    this.fromDestination = fromDestination;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (method.getDeclaringClass() == Object.class) {
      return method.invoke(this, args);
    }

    Class<?> declaringClass = method.getDeclaringClass();
    long namespaceId = hash(declaringClass.getPackage().getName());
    long classId = hash(declaringClass.getName());
    long methodId = hash(method.getName());
    Type type = method.getGenericReturnType();
    ParameterizedType parameterizedType = (ParameterizedType) type;
    Type[] typeArguments = parameterizedType.getActualTypeArguments();
    Class<?> returnType = (Class<?>) typeArguments[0];

    if (!method.getReturnType().isAssignableFrom(Flowable.class)) {
      throw new IllegalStateException("method must return " + Flowable.class.getCanonicalName());
    }

    Annotation[] annotations = method.getDeclaredAnnotations();
    for (Annotation annotation : annotations) {
      if (annotation instanceof FireForget) {
        long[] groupIds = GroupUtil.toGroupIdArray(group);
        FireForget fireforget = (FireForget) annotation;
        Object arg = args != null ? args[0] : null;
        Serializer<?> requestSerializer =
            arg != null ? Serializers.getSerializer(fireforget.serializer(), arg) : null;

        return rSocketPublishProcessor
            .take(1)
            .flatMap(
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
                      fromAccountId,
                      fromDestination,
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

                  return rSocket.fireAndForget(payload);
                });
      } else if (annotation instanceof RequestChannel) {
        long[] groupIds = GroupUtil.toGroupIdArray(group);
        RequestChannel requestchannel = (RequestChannel) annotation;
        Object arg = args != null ? args[0] : null;

        if (args == null) {
          throw new IllegalStateException("request channel must have an argument");
        }

        Class<?> requestType = ClassUtil.getParametrizedClass(arg.getClass());
        Serializer<?> requestSerializer =
            Serializers.getSerializer(requestchannel.serializer(), requestType);
        Serializer<?> responseSerializer =
            Serializers.getSerializer(requestchannel.serializer(), returnType);

        return rSocketPublishProcessor
            .take(1)
            .flatMap(
                rSocket -> {
                  Flowable<Payload> map =
                      Flowable.fromPublisher((Publisher) arg)
                          .map(
                              o -> {
                                ByteBuf route;
                                if (destination > 0) {
                                  int length =
                                      RouteDestinationFlyweight.computeLength(
                                          RouteType.STREAM_ID_ROUTE, groupIds.length);
                                  route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                  RouteDestinationFlyweight.encodeRouteByDestination(
                                      route,
                                      RouteType.STREAM_ID_ROUTE,
                                      accountId,
                                      destination,
                                      groupIds);
                                } else {
                                  int length =
                                      RouteDestinationFlyweight.computeLength(
                                          RouteType.STREAM_GROUP_ROUTE, groupIds.length);
                                  route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                  RouteDestinationFlyweight.encodeRouteByGroup(
                                      route, RouteType.STREAM_GROUP_ROUTE, accountId, groupIds);
                                }

                                int length =
                                    RoutingFlyweight.computeLength(true, false, false, route);

                                ByteBuf metadata =
                                    PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                RoutingFlyweight.encode(
                                    metadata,
                                    true,
                                    false,
                                    false,
                                    0,
                                    fromAccountId,
                                    fromDestination,
                                    0,
                                    namespaceId,
                                    classId,
                                    methodId,
                                    generator.nextId(),
                                    route);

                                ByteBuffer buffer = ByteBuffer.allocateDirect(metadata.capacity());
                                metadata.getBytes(0, buffer);
                                ByteBuffer data = requestSerializer.serialize(o);

                                return new PayloadImpl(data, buffer);
                              });

                  return rSocket
                      .requestChannel(map)
                      .map(
                          payload -> {
                            ByteBuffer data = payload.getData();
                            return responseSerializer.deserialize(data);
                          });
                });

      } else if (annotation instanceof RequestResponse) {
        long[] groupIds = GroupUtil.toGroupIdArray(group);
        RequestResponse requestresponse = (RequestResponse) annotation;
        Object arg = args != null ? args[0] : null;
        Serializer<?> requestSerializer =
            arg != null ? Serializers.getSerializer(requestresponse.serializer(), arg) : null;
        Serializer<?> responseSerializer =
            Serializers.getSerializer(requestresponse.serializer(), returnType);

        return rSocketPublishProcessor
            .take(1)
            .flatMap(
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

                  System.out.println(ByteBufUtil.prettyHexDump(route, 0, route.capacity()));

                  int length = RoutingFlyweight.computeLength(true, false, false, route);

                  ByteBuf metadata = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                  RoutingFlyweight.encode(
                      metadata,
                      true,
                      false,
                      false,
                      0,
                      fromAccountId,
                      fromDestination,
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

      } else if (annotation instanceof RequestStream) {
        long[] groupIds = GroupUtil.toGroupIdArray(group);
        RequestStream requeststream = (RequestStream) annotation;
        Object arg = args != null ? args[0] : null;
        Serializer<?> requestSerializer =
            arg != null ? Serializers.getSerializer(requeststream.serializer(), arg) : null;
        Serializer<?> responseSerializer =
            Serializers.getSerializer(requeststream.serializer(), returnType);

        return rSocketPublishProcessor
            .take(1)
            .flatMap(
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
                      fromAccountId,
                      fromDestination,
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

    throw new IllegalStateException("no method found with Service annotation");
  }
}
