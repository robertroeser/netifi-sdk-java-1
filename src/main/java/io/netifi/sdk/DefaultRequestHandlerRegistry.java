package io.netifi.sdk;

import io.netifi.sdk.annotations.FIRE_FORGET;
import io.netifi.sdk.annotations.REQUEST_CHANNEL;
import io.netifi.sdk.annotations.REQUEST_RESPONSE;
import io.netifi.sdk.annotations.REQUEST_STREAM;
import io.netifi.sdk.rs.RequestHandlerMetadata;
import io.netifi.sdk.serializer.Serializer;
import io.netifi.sdk.serializer.Serializers;
import io.netifi.sdk.util.ClassUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.netifi.sdk.util.HashUtil.hash;

/** */
public class DefaultRequestHandlerRegistry implements RequestHandlerRegistry {
  Map<String, RequestHandlerMetadata> cachedMethods;

  public DefaultRequestHandlerRegistry() {
    cachedMethods = new ConcurrentHashMap<>();
  }
  
  public synchronized <T1, T2> void registerHandler(Class<T1> clazz, T2 t) {
    if (!clazz.isInterface()) {
      throw new IllegalStateException("class must interface");
    }

    if (!clazz.isAssignableFrom(t.getClass())) {
      throw new IllegalStateException("class provided must implement the interface provided");
    }

    String packageName = clazz.getPackage().getName();
    String clazzName = clazz.getName();

    long namespaceId = hash(packageName);
    long classId = hash(clazzName);

    List<RequestHandlerMetadata> list = new ArrayList<>();
    try {
      Method[] methods = clazz.getDeclaredMethods();
      for (Method method : methods) {
        Class<?>[] parameters = method.getParameterTypes();
        if (parameters.length > 1) {
          throw new IllegalStateException("can only have 0 or 1 parameters");
        }
        
        Class<?> requestType = parameters.length > 0 ? parameters[0] : null;
        long methodId = hash(method.getName());
        Type type = method.getGenericReturnType();
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        Class<?> returnType = (Class<?>) typeArguments[0];
        Annotation[] annotations = method.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
          if (annotation instanceof FIRE_FORGET) {
            FIRE_FORGET fire_forget = (FIRE_FORGET) annotation;
            Serializer<?> requestSerializer =
                requestType != null
                    ? Serializers.getSerializer(fire_forget.serializer(), requestType)
                    : null;
            Serializer<?> responseSerializer = null;

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

          } else if (annotation instanceof REQUEST_CHANNEL) {
            REQUEST_CHANNEL request_channel = (REQUEST_CHANNEL) annotation;
            Serializer<?> requestSerializer =
                requestType != null
                    ? Serializers.getSerializer(
                        request_channel.serializer(), ClassUtil.getParametrizedClass(requestType))
                    : null;
            Serializer<?> responseSerializer =
                Serializers.getSerializer(request_channel.serializer(), returnType);

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
            REQUEST_RESPONSE request_response = (REQUEST_RESPONSE) annotation;
            Serializer<?> requestSerializer =
                requestType != null
                    ? Serializers.getSerializer(request_response.serializer(), requestType)
                    : null;
            Serializer<?> responseSerializer =
                Serializers.getSerializer(request_response.serializer(), returnType);

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
            REQUEST_STREAM request_stream = (REQUEST_STREAM) annotation;
            Serializer<?> requestSerializer =
                requestType != null
                    ? Serializers.getSerializer(request_stream.serializer(), requestType)
                    : null;
            Serializer<?> responseSerializer =
                Serializers.getSerializer(request_stream.serializer(), returnType);

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

  @Override
  public RequestHandlerMetadata lookup(String name) {
    return cachedMethods.get(name);
  }

  
}
