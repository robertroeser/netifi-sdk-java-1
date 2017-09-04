package io.netifi.sdk.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/** */
public class ClassUtil {
  public static Class<?> getParameterizedClass(Class<?> clazz) throws Exception {
    ParameterizedType parameterizedType = (ParameterizedType) clazz.getGenericInterfaces()[0];
    Type[] typeArguments = parameterizedType.getActualTypeArguments();
    Class<?> typeArgument = (Class<?>) typeArguments[0];
    return typeArgument;
  }
}
