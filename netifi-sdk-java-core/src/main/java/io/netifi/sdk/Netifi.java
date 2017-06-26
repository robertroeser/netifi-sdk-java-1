package io.netifi.sdk;

import reactor.core.publisher.Mono;

/** */
public interface Netifi {
  static Netifi newInstance() {
    return newInstance(new NetifiConfig());
  }

  static Netifi newInstance(NetifiConfig netifiConfig) {
    return new DefaultNetifi(netifiConfig);
  }

  <T> Mono<T> locateById(Class<T> clazz, String id);

  <T> Mono<T> locateByGroup(Class<T> clazz, String group);

  <T> Mono<T> locate(Class<T> clazz, String group, String subgroup);

  <T> void register(Class<T> clazz);
}
