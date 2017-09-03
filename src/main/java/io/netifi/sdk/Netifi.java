package io.netifi.sdk;

import io.netifi.nrqp.frames.RouteDestinationFlyweight;
import io.netifi.nrqp.frames.RouteType;
import io.netifi.sdk.annotations.FIRE_FORGET;
import io.netifi.sdk.annotations.REQUEST_CHANNEL;
import io.netifi.sdk.annotations.REQUEST_RESPONSE;
import io.netifi.sdk.annotations.REQUEST_STREAM;
import io.netifi.sdk.rs.RequestHandlerMetadata;
import io.netifi.sdk.rs.RequestHandlingRSocket;
import io.netifi.sdk.serializer.Serializer;
import io.netifi.sdk.util.GroupUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.reactivex.Flowable;
import io.reactivex.processors.ReplayProcessor;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.PayloadImpl;
import net.openhft.hashing.LongHashFunction;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;

import javax.xml.bind.DatatypeConverter;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** This is where the magic happens */
public class Netifi implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(Netifi.class);

  private static final LongHashFunction xx = LongHashFunction.xx();

  private String host;
  private int port;
  private long accessKey;
  private long accountId;
  private long[] groupIds;
  private String destination;
  private long destinationId;
  private String address;
  private String accessToken;
  private byte[] accessTokenBytes;
  private volatile boolean running = true;

  private ConcurrentHashMap<String, RequestHandlerMetadata> cachedMethods;

  private ConcurrentHashMap<Class<?>, Serializer> serializersMap;

  private ReplayProcessor<RSocket> rSocketPublishProcessor;

  private volatile Disposable disposable;

  public Netifi(
      String host,
      int port,
      long accessKey,
      long accountId,
      long[] groupIds,
      String destination,
      long destinationId,
      String address,
      String accessToken,
      byte[] accessTokenBytes) {
    this.host = host;
    this.port = port;
    this.accessKey = accessKey;
    this.accountId = accountId;
    this.groupIds = groupIds;
    this.destination = destination;
    this.destinationId = destinationId;
    this.address = address;
    this.accessToken = accessToken;
    this.accessTokenBytes = accessTokenBytes;
    this.rSocketPublishProcessor = ReplayProcessor.create(1);
    this.serializersMap = new ConcurrentHashMap<>();
    this.disposable =
        RSocketFactory.connect()
            .errorConsumer(throwable -> logger.error("unhandled error", throwable))
            .acceptor(
                rSocket -> {
                  rSocketPublishProcessor.onNext(rSocket);
                  return new RequestHandlingRSocket(cachedMethods);
                })
            .transport(TcpClientTransport.create(host, port))
            .start()
            .retry(throwable -> running)
            .subscribe();
  }

  /**
   * Routes to a group
   *
   * @param service
   * @param accountId
   * @param group
   * @param <T>
   * @return
   */
  public <T> T create(Class<T> service, long accountId, String group, long destination) {
    Object o =
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {service},
            new InvocationHandler() {
              @Override
              public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                  return method.invoke(this, args);
                }

                Class<?> returnType = method.getReturnType();
                Annotation[] annotations = method.getAnnotations();
                for (Annotation annotation : annotations) {
                  if (annotation instanceof FIRE_FORGET) {
                    long[] groupIds = GroupUtil.toGroupIdArray(group);
                    validate(method, args);
                    FIRE_FORGET fire_forget = (FIRE_FORGET) annotation;
                    Class<Serializer<?>> serializer = fire_forget.serializer();
                    Constructor<Serializer<?>> serializerConstructor =
                        serializer.getDeclaredConstructor(Class.class);
                    Object arg = args[0];
                    Serializer<?> requestSerializer =
                        serializerConstructor.newInstance(arg.getClass());

                    return rSocketPublishProcessor.flatMap(
                        rSocket -> {
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

                          return rSocket.fireAndForget(payload);
                        });
                  } else if (annotation instanceof REQUEST_CHANNEL) {
                    long[] groupIds = GroupUtil.toGroupIdArray(group);
                    validate(method, args);
                    REQUEST_CHANNEL request_channel = (REQUEST_CHANNEL) annotation;
                    Class<Serializer<?>> serializer = request_channel.serializer();
                    Constructor<Serializer<?>> serializerConstructor =
                        serializer.getDeclaredConstructor(Class.class);
                    Object arg = args[0];
                    Serializer<?> requestSerializer =
                        serializerConstructor.newInstance(arg.getClass());
                    Serializer<?> responseSerializer =
                        serializerConstructor.newInstance(getParameterizedClass(returnType));

                    rSocketPublishProcessor.flatMap(
                        rSocket -> {
                          Flowable<Payload> map =
                              Flowable.fromPublisher((Publisher) arg)
                                  .map(
                                      o -> {
                                        ByteBuffer data = requestSerializer.serialize(o);
                                        int length =
                                            RouteDestinationFlyweight.computeLength(
                                                RouteType.STREAM_ID_ROUTE, groupIds.length);
                                        ByteBuf byteBuf =
                                            PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                        if (destination > 0) {
                                          RouteDestinationFlyweight.encodeRouteByDestination(
                                              byteBuf,
                                              RouteType.STREAM_ID_ROUTE,
                                              accountId,
                                              destination,
                                              groupIds);
                                        } else {
                                          RouteDestinationFlyweight.encodeRouteByGroup(
                                              byteBuf,
                                              RouteType.STREAM_GROUP_ROUTE,
                                              accountId,
                                              groupIds);
                                        }

                                        ByteBuffer byteBuffer =
                                            ByteBuffer.allocateDirect(byteBuf.capacity());
                                        byteBuf.getBytes(0, byteBuffer);

                                        PayloadImpl payload =
                                            new PayloadImpl(
                                                requestSerializer.serialize(arg), byteBuffer);

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
                    validate(method, args);
                    REQUEST_RESPONSE request_response = (REQUEST_RESPONSE) annotation;
                    Class<Serializer<?>> serializer = request_response.serializer();
                    Constructor<Serializer<?>> serializerConstructor =
                        serializer.getDeclaredConstructor(Class.class);
                    Object arg = args[0];
                    Serializer<?> requestSerializer =
                        serializerConstructor.newInstance(arg.getClass());
                    Serializer<?> responseSerializer =
                        serializerConstructor.newInstance(getParameterizedClass(returnType));

                    rSocketPublishProcessor.flatMap(
                        rSocket -> {
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

                          return rSocket
                              .requestResponse(payload)
                              .map(
                                  payload1 -> {
                                    ByteBuffer data = payload1.getData();
                                    return responseSerializer.deserialize(data);
                                  });
                        });

                  } else if (annotation instanceof REQUEST_STREAM) {
                    long[] groupIds = GroupUtil.toGroupIdArray(group);
                    validate(method, args);
                    REQUEST_STREAM request_stream = (REQUEST_STREAM) annotation;
                    Class<Serializer<?>> serializer = request_stream.serializer();
                    Constructor<Serializer<?>> serializerConstructor =
                        serializer.getDeclaredConstructor(Class.class);
                    Object arg = args[0];
                    Serializer<?> requestSerializer =
                        serializerConstructor.newInstance(arg.getClass());
                    Serializer<?> responseSerializer =
                        serializerConstructor.newInstance(getParameterizedClass(returnType));

                    rSocketPublishProcessor.flatMap(
                        rSocket -> {
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

                          return rSocket
                              .requestStream(payload)
                              .map(
                                  payload1 -> {
                                    ByteBuffer data = payload1.getData();
                                    return responseSerializer.deserialize(data);
                                  });
                        });
                  }
                }

                throw new IllegalStateException("no method found with netifi annotation");
              }
            });

    return (T) o;
  }

  private void validate(Method method, Object[] args) {
    if (args.length != 1) {
      throw new IllegalStateException("methods can only have one argument");
    }

    Class<?> returnType = method.getReturnType();

    if (!returnType.isAssignableFrom(Flowable.class)) {
      throw new IllegalStateException("methods must return " + Flowable.class.getCanonicalName());
    }
  }

  public <T> void registerHandler(T t, Class<T> clazz) {
    if (clazz.isInterface()) {
      throw new IllegalArgumentException("must a class");
    }

    String packageName = clazz.getPackage().getName();
    String clazzName = clazz.getName();

    long namespaceId = xx.hashChars(packageName);
    long classId = xx.hashChars(clazzName);

    List<RequestHandlerMetadata> list = new ArrayList<>();
    try {
      Method[] methods = clazz.getMethods();
      for (Method method : methods) {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
          if (annotation instanceof FIRE_FORGET) {
            long methodId = xx.hashChars(method.getName());

            FIRE_FORGET fire_forget = (FIRE_FORGET) annotation;
            Class<Serializer<?>> serializerClass = fire_forget.serializer();
            Class<?> returnType = getParameterizedClass(method.getReturnType());

            if (returnType.isAssignableFrom(Void.class)) {
              throw new IllegalStateException(
                  "method "
                      + method.getName()
                      + " should return void if its annotated with fired forget");
            }

            Constructor<Serializer<?>> constructor = serializerClass.getConstructor(Class.class);
            Serializer<?> requestSerializer = constructor.newInstance(returnType);

            RequestHandlerMetadata handlerMetadata =
                new RequestHandlerMetadata(
                    requestSerializer, null, method, clazz, t, namespaceId, classId, methodId);

            list.add(handlerMetadata);
            break;

          } else if (annotation instanceof REQUEST_CHANNEL) {
            long methodId = xx.hashChars(method.getName());
            REQUEST_CHANNEL request_channel = (REQUEST_CHANNEL) annotation;
            Class<Serializer<?>> serializerClass = request_channel.serializer();

            Class<?> returnType = getParameterizedClass(method.getReturnType());
            Constructor<Serializer<?>> constructor = serializerClass.getConstructor(Class.class);
            Serializer<?> requestSerializer = constructor.newInstance(returnType);

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
              throw new IllegalStateException(
                  "method " + method.getName() + " should take one argument");
            }
            Class<?> requestType = parameterTypes[0];
            Serializer<?> responseSerializer =
                constructor.newInstance(getParameterizedClass(requestType));

            RequestHandlerMetadata handlerMetadata =
                new RequestHandlerMetadata(
                    requestSerializer,
                    responseSerializer,
                    method,
                    clazz,
                    t,
                    namespaceId,
                    classId,
                    methodId);

            list.add(handlerMetadata);
            break;
          } else if (annotation instanceof REQUEST_RESPONSE) {
            long methodId = xx.hashChars(method.getName());
            REQUEST_RESPONSE request_response = (REQUEST_RESPONSE) annotation;
            Class<Serializer<?>> serializerClass = request_response.serializer();

            Class<?> returnType = getParameterizedClass(method.getReturnType());
            Constructor<Serializer<?>> constructor = serializerClass.getConstructor(Class.class);
            Serializer<?> requestSerializer = constructor.newInstance(returnType);

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
              throw new IllegalStateException(
                  "method " + method.getName() + " should take one argument");
            }
            Class<?> requestType = parameterTypes[0];
            Serializer<?> responseSerializer = constructor.newInstance(requestType);

            RequestHandlerMetadata handlerMetadata =
                new RequestHandlerMetadata(
                    requestSerializer,
                    responseSerializer,
                    method,
                    clazz,
                    t,
                    namespaceId,
                    classId,
                    methodId);

            list.add(handlerMetadata);
            break;
          } else if (annotation instanceof REQUEST_STREAM) {
            long methodId = xx.hashChars(method.getName());
            REQUEST_STREAM request_stream = (REQUEST_STREAM) annotation;
            Class<Serializer<?>> serializerClass = request_stream.serializer();
            Class<?> returnType = getParameterizedClass(method.getReturnType());

            Constructor<Serializer<?>> constructor = serializerClass.getConstructor(Class.class);
            Serializer<?> requestSerializer = constructor.newInstance(returnType);

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
              throw new IllegalStateException(
                  "method " + method.getName() + " should take one argument");
            }
            Class<?> requestType = parameterTypes[0];
            Serializer<?> responseSerializer = constructor.newInstance(requestType);

            RequestHandlerMetadata handlerMetadata =
                new RequestHandlerMetadata(
                    requestSerializer,
                    responseSerializer,
                    method,
                    clazz,
                    t,
                    namespaceId,
                    classId,
                    methodId);

            list.add(handlerMetadata);
            break;
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (list.isEmpty()) {
      throw new IllegalArgumentException("no methods annotated with Netifi annotations found");
    }

    list.forEach(metadata -> cachedMethods.putIfAbsent(metadata.getStringKey(), metadata));
  }

  private Class<?> getParameterizedClass(Class<?> clazz) throws Exception {
    Type superclassType = clazz.getGenericSuperclass();
    if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
      return null;
    }

    Type[] actualTypeArguments = ((ParameterizedType) superclassType).getActualTypeArguments();
    Class<?> aClass =
        Class.forName(
            actualTypeArguments[0].getTypeName(),
            false,
            Thread.currentThread().getContextClassLoader());
    return aClass;
  }

  @Override
  public void close() throws Exception {
    if (disposable != null) {
      disposable.dispose();
    }
    running = false;
  }

  public static class Builder {
    private String host;
    private Integer port;
    private Long accessKey;
    private Long accountId;
    private long[] groupIds;
    private String destination;
    private Long destinationId;
    private String address = null;
    private String accessToken = null;
    private byte[] accessTokenBytes = new byte[20];

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder accessKey(long accessKey) {
      this.accessKey = accessKey;
      return this;
    }

    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      this.accessTokenBytes = DatatypeConverter.parseBase64Binary(accessToken);
      return this;
    }

    private Builder address(String address) {
      this.address = address;

      int pos = address.lastIndexOf(':');
      this.accountId = Long.valueOf(address.substring(0, pos));

      String[] split = address.substring(pos + 1, address.length()).split(".");
      groupIds = new long[split.length];

      for (int i = 0; i < split.length; i++) {
        groupIds[i] = Math.abs(xx.hashChars(split[i]));
      }

      return this;
    }

    private Builder destination(String destination) {
      this.destination = destination;
      this.destinationId = xx.hashChars(destination);
      return this;
    }

    public Netifi build() {
      Objects.requireNonNull(host, "host is required");
      Objects.requireNonNull(port, "port is required");
      Objects.requireNonNull(address, "address is required");
      Objects.requireNonNull(destinationId, "destination id is required");

      return new Netifi(
          host,
          port,
          accessKey,
          accountId,
          groupIds,
          destination,
          destinationId,
          address,
          accessToken,
          accessTokenBytes);
    }
  }
}
