package io.netifi.sdk.connection;

import io.netifi.sdk.NetifiConfig;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** */
public final class Connectors {
  private Connectors() {}

  public abstract static class ConnectorProxy<R extends Connection> implements Connector<R> {
    protected Connector<R> proxied;

    public ConnectorProxy(Connector<R> proxied) {
      Objects.nonNull(proxied);

      this.proxied = proxied;
    }
  }

  public static class CachingConnector<R extends Connection> extends ConnectorProxy<R> {
    private final AtomicReference<MonoProcessor<R>> cachedConnection;

    public CachingConnector(Connector<R> proxied) {
      super(proxied);
      this.cachedConnection = new AtomicReference<>();
    }

    @Override
    public Mono<R> apply(NetifiConfig netifiConfig) {
      return Mono.defer(
          () -> {
            if (cachedConnection.compareAndSet(null, MonoProcessor.create())) {
              proxied
                  .apply(netifiConfig)
                  .doOnSuccess(
                      connection -> connection.onClose().subscribe(v -> cachedConnection.set(null)))
                  .subscribe(cachedConnection.get());
            }

            return cachedConnection.get();
          });
    }
  }
}
