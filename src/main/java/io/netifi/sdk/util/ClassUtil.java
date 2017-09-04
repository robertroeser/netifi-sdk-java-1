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


/*
public Class getGenericInterfaceType(){
        Class clazz = getClass();
        ParameterizedType parameterizedType = (ParameterizedType) clazz.getGenericInterfaces()[0];
        Type[] typeArguments = parameterizedType.getActualTypeArguments();
        Class<?> typeArgument = (Class<?>) typeArguments[0];
        return typeArgument;
    }
 */

/*
Type superclassType = clazz.getGenericSuperclass();
    String typeName = superclassType.getTypeName();
    String name = clazz.getName();
    System.out.println(name);
    /*if (!ParameterizedType.class.isAssignableFrom(superclassType.getClass())) {
      return null;
    }
  
  Type[] actualTypeArguments = ((ParameterizedType) superclassType).getActualTypeArguments();
  Class<?> aClass =
      Class.forName(
          actualTypeArguments[0].getTypeName(),
          false,
          Thread.currentThread().getContextClassLoader());
    return aClass;
 */