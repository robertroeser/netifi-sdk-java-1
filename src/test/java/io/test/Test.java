package io.test;

import reactor.core.publisher.Flux;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/** Created by robertroeser on 9/2/17. */
public class Test {
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
  
  @org.junit.Test
  public void test() throws Exception {
    Flux<Integer> range = Flux.range(1, 10);
    Class<?> parameterizedClass = getParameterizedClass(range.getClass());
    System.out.println(parameterizedClass.getName());
  }
}
