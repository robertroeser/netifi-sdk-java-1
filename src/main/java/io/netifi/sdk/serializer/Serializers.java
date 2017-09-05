package io.netifi.sdk.serializer;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** */
public final class Serializers {
  public static final String DEFAULT_SERIALIZER_PROPERTY = "netifi.serializer.default";
  public static final String CBOR = "io.netifi.sdk.serializer.CBORSerializer";
  public static final String BINARY = "io.netifi.sdk.serializer.ByteBufferSerializer";
  public static final String JSON = "io.netifi.sdk.serializer.JSONSerializer";

  public static final String DEFAULT;

  private static final Map<String, Class<? extends Serializer>> SERIALIZER_CLASSES;

  private static final Map<Class, Serializer<?>> SERIALIZERS;

  static {
    DEFAULT = System.getProperty(DEFAULT_SERIALIZER_PROPERTY, JSON);
    SERIALIZER_CLASSES = new ConcurrentHashMap<>();
    SERIALIZERS = new ConcurrentHashMap<>();
  }

  private Serializers() {}
  
  @SuppressWarnings("unchecked")
  public static Class<? extends Serializer> getSerializerClass(String className) {
    return SERIALIZER_CLASSES.computeIfAbsent(
        className,
        n -> {
          try {
            if (className.isEmpty()) {
              return (Class<? extends Serializer>)
                  Class.forName(
                      Serializers.DEFAULT, true, Thread.currentThread().getContextClassLoader());
            } else {
              return (Class<? extends Serializer>)
                  Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  public static <T> Serializer<T> getSerializer(String className, Object target) {
    Class<? extends Serializer> serializerClass = getSerializerClass(className);
    return getSerializer(serializerClass, target.getClass());
  }

  public static <T> Serializer<T> getSerializer(String className, Class target) {
    Class<? extends Serializer> serializerClass = getSerializerClass(className);
    return getSerializer(serializerClass, target);
  }

  @SuppressWarnings("unchecked")
  public static <T> Serializer<T> getSerializer(
      Class<? extends Serializer> serializerClass, Class target) {

    return (Serializer<T>)
        SERIALIZERS.computeIfAbsent(
            target,
            c -> {
              try {
                Constructor<? extends Serializer> serializerConstructor =
                    serializerClass.getDeclaredConstructor(Class.class);

                Serializer serializer = serializerConstructor.newInstance(target);
                return serializer;
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }
}
