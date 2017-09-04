package io.netifi.sdk;

import io.netifi.sdk.rs.RequestHandlerMetadata;

/** */
public interface RequestHandlerRegistry {
  <T1, T2> void registerHandler(Class<T1> clazz, T2 t);
  
  RequestHandlerMetadata lookup(String name);
}
