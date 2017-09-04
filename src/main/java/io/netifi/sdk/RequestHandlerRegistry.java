package io.netifi.sdk;

import io.netifi.sdk.rs.RequestHandlerMetadata;

/** */
public interface RequestHandlerRegistry {
  <T> void registerHandler(T t);
  
  RequestHandlerMetadata lookup(String name);
}
