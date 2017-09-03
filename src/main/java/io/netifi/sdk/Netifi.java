package io.netifi.sdk;

import io.netifi.sdk.annotations.FIRE_FORGET;
import io.netifi.sdk.annotations.REQUEST_CHANNEL;
import io.netifi.sdk.annotations.REQUEST_RESPONSE;
import io.netifi.sdk.annotations.REQUEST_STREAM;
import io.netifi.sdk.rs.RequestHandlerMetadata;
import io.netifi.sdk.serializer.Serializer;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import net.openhft.hashing.LongHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** This is where the magic happens */
public class Netifi {
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

  private ConcurrentHashMap<String, RequestHandlerMetadata> cachedMethods;

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
  
    RSocketFactory
        .receive()
        .errorConsumer(throwable -> logger.error("unhandled error", throwable));
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
