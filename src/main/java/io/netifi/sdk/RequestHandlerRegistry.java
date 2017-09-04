package io.netifi.sdk;

import io.netifi.sdk.rs.RequestHandlerMetadata;

/** */
public interface RequestHandlerRegistry {
  <T> void registerHandler(T t, Class<T> clazz);
  
  RequestHandlerMetadata lookup(String name);
}
