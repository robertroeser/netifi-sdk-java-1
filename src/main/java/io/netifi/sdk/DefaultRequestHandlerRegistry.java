package io.netifi.sdk;

import io.netifi.sdk.annotations.FIRE_FORGET;
import io.netifi.sdk.annotations.REQUEST_CHANNEL;
import io.netifi.sdk.annotations.REQUEST_RESPONSE;
import io.netifi.sdk.annotations.REQUEST_STREAM;
import io.netifi.sdk.rs.RequestHandlerMetadata;
import io.netifi.sdk.serializer.Serializer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static io.netifi.sdk.util.HashUtil.hash;

/** */
public class DefaultRequestHandlerRegistry implements RequestHandlerRegistry {
  private ConcurrentHashMap<String, RequestHandlerMetadata> cachedMethods;

  public DefaultRequestHandlerRegistry() {}

  @Override
  public synchronized <T> void registerHandler(T t, Class<T> clazz) {
    if (clazz.isInterface()) {
      throw new IllegalArgumentException("must a class");
    }

    String packageName = clazz.getPackage().getName();
    String clazzName = clazz.getName();

    long namespaceId = hash(packageName);
    long classId = hash(clazzName);

    List<RequestHandlerMetadata> list = new ArrayList<>();
    try {
      Method[] methods = clazz.getMethods();
      for (Method method : methods) {
        Annotation[] annotations = method.getAnnotations();
        for (Annotation annotation : annotations) {
          if (annotation instanceof FIRE_FORGET) {
            long methodId = hash(method.getName());

            FIRE_FORGET fire_forget = (FIRE_FORGET) annotation;
            Class<? extends Serializer> serializerClass =
                (Class<? extends Serializer>)
                    Class.forName(
                        fire_forget.serializer(),
                        true,
                        Thread.currentThread().getContextClassLoader());
            Class<?> returnType = getParametrizedClass(method.getReturnType());

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
            long methodId = hash(method.getName());
            REQUEST_CHANNEL request_channel = (REQUEST_CHANNEL) annotation;
            Class<? extends Serializer> serializerClass =
                (Class<? extends Serializer>)
                    Class.forName(
                        request_channel.serializer(),
                        true,
                        Thread.currentThread().getContextClassLoader());

            Class<?> returnType = getParametrizedClass(method.getReturnType());
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
                constructor.newInstance(getParametrizedClass(requestType));

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
            long methodId = hash(method.getName());
            REQUEST_RESPONSE request_response = (REQUEST_RESPONSE) annotation;
            Class<? extends Serializer> serializerClass =
                (Class<? extends Serializer>)
                    Class.forName(
                        request_response.serializer(),
                        true,
                        Thread.currentThread().getContextClassLoader());

            Class<?> returnType = getParametrizedClass(method.getReturnType());
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
            long methodId = hash(method.getName());
            REQUEST_STREAM request_stream = (REQUEST_STREAM) annotation;
            Class<? extends Serializer> serializerClass =
                (Class<? extends Serializer>)
                    Class.forName(
                        request_stream.serializer(),
                        true,
                        Thread.currentThread().getContextClassLoader());
            Class<?> returnType = getParametrizedClass(method.getReturnType());

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

  @Override
  public RequestHandlerMetadata lookup(String name) {

    return null;
  }

  private Class<?> getParametrizedClass(Class<?> clazz) throws Exception {
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
}
