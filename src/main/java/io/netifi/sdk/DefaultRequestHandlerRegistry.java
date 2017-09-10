package io.netifi.sdk;

import io.netifi.sdk.annotations.FireForget;
import io.netifi.sdk.annotations.RequestChannel;
import io.netifi.sdk.annotations.RequestResponse;
import io.netifi.sdk.annotations.RequestStream;
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
          if (annotation instanceof FireForget) {
            FireForget fireforget = (FireForget) annotation;
            Serializer<?> requestSerializer =
                requestType != null
                    ? Serializers.getSerializer(fireforget.serializer(), requestType)
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

          } else if (annotation instanceof RequestChannel) {
            RequestChannel requestchannel = (RequestChannel) annotation;
            Serializer<?> requestSerializer =
                requestType != null
                    ? Serializers.getSerializer(
                        requestchannel.serializer(), ClassUtil.getParametrizedClass(requestType))
                    : null;
            Serializer<?> responseSerializer =
                Serializers.getSerializer(requestchannel.serializer(), returnType);

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
          } else if (annotation instanceof RequestResponse) {
            RequestResponse requestresponse = (RequestResponse) annotation;
            Serializer<?> requestSerializer =
                requestType != null
                    ? Serializers.getSerializer(requestresponse.serializer(), requestType)
                    : null;
            Serializer<?> responseSerializer =
                Serializers.getSerializer(requestresponse.serializer(), returnType);

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
          } else if (annotation instanceof RequestStream) {
            RequestStream requeststream = (RequestStream) annotation;
            Serializer<?> requestSerializer =
                requestType != null
                    ? Serializers.getSerializer(requeststream.serializer(), requestType)
                    : null;
            Serializer<?> responseSerializer =
                Serializers.getSerializer(requeststream.serializer(), returnType);

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
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    if (list.isEmpty()) {
      throw new IllegalArgumentException("no methods annotated with Service annotations found");
    }

    list.forEach(metadata -> cachedMethods.putIfAbsent(metadata.getStringKey(), metadata));
  }

  @Override
  public RequestHandlerMetadata lookup(String name) {
    return cachedMethods.get(name);
  }

  
}
