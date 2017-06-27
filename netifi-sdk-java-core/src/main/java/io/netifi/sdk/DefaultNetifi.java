package io.netifi.sdk;

import io.netifi.sdk.connection.Connectors;
import io.netifi.sdk.connection.tcp.TcpConnection;
import io.netifi.sdk.connection.tcp.TcpConnector;
import io.netifi.sdk.service.ServiceHandler;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/** */
class DefaultNetifi extends Netifi implements Closeable {
  private NetifiConfig config;
  private Connectors.CachingConnector<TcpConnection> cachingConnector;
  private MonoProcessor<Void> onClose;
  private ServiceHandler handler;
  
  public DefaultNetifi(NetifiConfig config) {
    this.config = config;
    this.onClose = MonoProcessor.create();
    this.cachingConnector = new Connectors.CachingConnector<>(new TcpConnector());
    this.handler = new ServiceHandler();
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
  public <T> T findServiceById(Class<T> clazz, String id) {
    return null;
  }
  
  @Override
  public <T> T findService(Class<T> clazz, String group) {
    return null;
  }
  
  @Override
  public <T> T findService(Class<T> clazz, String group, String subgroup) {
    return null;
  }
  
  @Override
  public Mono<Boolean> isIdConnected(String id) {
    return null;
  }
  
  @Override
  public Mono<Boolean> isConnected(String group) {
    return null;
  }
  
  @Override
  public Mono<Boolean> isConnected(String group, String subgroup) {
    return null;
  }
  
  @Override
  public <T> void register(T t) {
    handler.register(t);
  }
  
  
}
