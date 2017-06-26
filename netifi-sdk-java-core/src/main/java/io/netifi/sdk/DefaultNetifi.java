package io.netifi.sdk;

import io.netifi.sdk.connection.Connectors;
import io.netifi.sdk.connection.tcp.TcpConnection;
import io.netifi.sdk.connection.tcp.TcpConnector;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/** */
class DefaultNetifi implements Netifi, Closeable {
  private NetifiConfig config;
  private Connectors.CachingConnector<TcpConnection> cachingConnector;
  private MonoProcessor<Void> onClose;

  public DefaultNetifi(NetifiConfig config) {
    this.config = config;
    this.onClose = MonoProcessor.create();
    this.cachingConnector = new Connectors.CachingConnector<>(new TcpConnector());
  }

  @Override
  public Mono<Void> close() {
    return Mono.empty().doFinally(s -> onClose.onComplete()).then();
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }
  
  @Override
  public <T> Mono<T> locateById(Class<T> clazz, String id) {
    return null;
  }
  
  @Override
  public <T> Mono<T> locateByGroup(Class<T> clazz, String group) {
    return null;
  }
  
  @Override
  public <T> Mono<T> locate(Class<T> clazz, String group, String subgroup) {
    return null;
  }
  
  @Override
  public <T> void register(Class<T> clazz) {
  
  }
}
