package io.netifi.sdk;

import io.netifi.sdk.annotations.FIRE_FORGET;
import io.netifi.sdk.annotations.REQUEST_CHANNEL;
import io.netifi.sdk.annotations.REQUEST_RESPONSE;
import io.netifi.sdk.annotations.REQUEST_STREAM;
import io.netifi.sdk.rs.RequestHandlerMetadata;
import io.netifi.sdk.rs.RequestHandlingRSocket;
import io.netifi.sdk.serializer.Serializer;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.reactivex.Flowable;
import io.reactivex.processors.ReplayProcessor;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import net.openhft.hashing.LongHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;

import javax.xml.bind.DatatypeConverter;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
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
  private String group;
  private String accessToken;
  private byte[] accessTokenBytes;
  private final TimebasedIdGenerator idGenerator;
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
      String group,
      String accessToken,
      byte[] accessTokenBytes) {
    this.host = host;
    this.port = port;
    this.accessKey = accessKey;
    this.accountId = accountId;
    this.groupIds = groupIds;
    this.destination = destination;
    this.destinationId = destinationId;
    this.group = group;
    this.accessToken = accessToken;
    this.accessTokenBytes = accessTokenBytes;
    this.rSocketPublishProcessor = ReplayProcessor.create(1);
    this.serializersMap = new ConcurrentHashMap<>();
    this.idGenerator = new TimebasedIdGenerator((int) destinationId);
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

  public <T> T create(Class<T> service, long accountId, String group) {
    return create(service, accountId, group, -1);
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
            new NetifiInvocationHandler(rSocketPublishProcessor, accountId, group, destination, idGenerator));

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
            Class<? extends Serializer> serializerClass =
                (Class<? extends Serializer>)
                    Class.forName(
                        fire_forget.serializer(),
                        true,
                        Thread.currentThread().getContextClassLoader());
            Class<?> returnType = getParameterizedClass(method.getReturnType());

            if (returnType.isAssignableFrom(Void.class)) {
              throw new IllegalStateException(
                  "method "
                      + method.getName()
                      + " should return void if its annotated with fired forget");
            }

            Constructor<? extends Serializer> constructor =
                serializerClass.getConstructor(Class.class);
            Serializer<?> requestSerializer = constructor.newInstance(returnType);

            RequestHandlerMetadata handlerMetadata =
                new RequestHandlerMetadata(
                    requestSerializer, null, method, clazz, t, namespaceId, classId, methodId);

            list.add(handlerMetadata);
            break;

          } else if (annotation instanceof REQUEST_CHANNEL) {
            long methodId = xx.hashChars(method.getName());
            REQUEST_CHANNEL request_channel = (REQUEST_CHANNEL) annotation;
            Class<? extends Serializer> serializerClass =
                (Class<? extends Serializer>)
                    Class.forName(
                        request_channel.serializer(),
                        true,
                        Thread.currentThread().getContextClassLoader());

            Class<?> returnType = getParameterizedClass(method.getReturnType());
            Constructor<? extends Serializer> constructor =
                serializerClass.getConstructor(Class.class);
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
            Class<? extends Serializer> serializerClass =
                (Class<? extends Serializer>)
                    Class.forName(
                        request_response.serializer(),
                        true,
                        Thread.currentThread().getContextClassLoader());

            Class<?> returnType = getParameterizedClass(method.getReturnType());
            Constructor<? extends Serializer> constructor =
                serializerClass.getConstructor(Class.class);
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
            Class<? extends Serializer> serializerClass =
                (Class<? extends Serializer>)
                    Class.forName(
                        request_stream.serializer(),
                        true,
                        Thread.currentThread().getContextClassLoader());
            Class<?> returnType = getParameterizedClass(method.getReturnType());

            Constructor<? extends Serializer> constructor =
                serializerClass.getConstructor(Class.class);
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
    private String group;
    private long[] groupIds;
    private String destination;
    private Long destinationId;
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

    public Builder group(String group) {
      String[] split = group.split(".");
      this.groupIds = new long[split.length];

      for (int i = 0; i < split.length; i++) {
        groupIds[i] = Math.abs(xx.hashChars(split[i]));
      }

      return this;
    }

    public Builder accountId(long accountId) {
      this.accountId = accountId;
      return this;
    }

    public Netifi build() {
      Objects.requireNonNull(host, "host is required");
      Objects.requireNonNull(port, "port is required");
      Objects.requireNonNull(accountId, "account Id is required");
      Objects.requireNonNull(group, "group is required");
      Objects.requireNonNull(destinationId, "destination id is required");

      return new Netifi(
          host,
          port,
          accessKey,
          accountId,
          groupIds,
          destination,
          destinationId,
          group,
          accessToken,
          accessTokenBytes);
    }
  }
}
