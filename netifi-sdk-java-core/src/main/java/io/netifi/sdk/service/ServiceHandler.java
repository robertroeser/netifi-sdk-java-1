package io.netifi.sdk.service;

import io.netifi.edge.router.Route;
import io.netifi.sdk.connection.Handler;
import io.netifi.sdk.serialization.RawPayload;
import io.netty.util.collection.LongObjectHashMap;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

/**
 * Maps an incoming request to a namespace using the incoming requests metadata.
 */
public class ServiceHandler implements Handler {
  private LongObjectHashMap<Handler> handlers;

  public ServiceHandler() {
    this.handlers = new LongObjectHashMap<>();
  }
    
    /**
     * Registers a service with the ServiceHandler so that incoming requests can be mapped to it
     * @param clazz The class that is the implementation of the service you want to register
     * @param <T>
     */
  public <T> void register(Class<T> clazz) {
    try {
      Package aPackage = clazz.getPackage();
      String namespace = aPackage.getName() + "namespace";
      Class<?> namespaceClass =
          Class.forName(namespace, false, Thread.currentThread().getContextClassLoader());
      Namespace annotation = namespaceClass.getAnnotation(Namespace.class);
      long id = annotation.id();
      Constructor<?> constructor = namespaceClass.getConstructor(null);
      Handler handler = (Handler) constructor.newInstance();

      synchronized (ServiceHandler.this) {
        handlers.put(id, handler);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Handler findHandler(RawPayload payload) {
    ByteBuffer metadata = payload.getMetadata();
    Route route = Route.getRootAsRoute(metadata);
    long id = route.destNamespace();
    Handler handler;
    synchronized (ServiceHandler.this) {
      handler = handlers.get(id);
    }

    if (handler == null) {
      throw new IllegalStateException("no handler found for namespace with id " + id);
    }

    return handler;
  }

  @Override
  public Mono<Void> fireAndForget(RawPayload payload) {
    try {
      return findHandler(payload).fireAndForget(payload);
    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Mono<RawPayload> requestResponse(RawPayload payload) {
    try {
      return findHandler(payload).requestResponse(payload);
    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Flux<RawPayload> requestStream(RawPayload payload) {
    try {
      return findHandler(payload).requestStream(payload);
    } catch (Throwable t) {
      return Flux.error(t);
    }
  }

  @Override
  public Flux<RawPayload> requestChannel(Publisher<RawPayload> payloads) {
    try {
      return Flux.from(payloads)
          .groupBy(
              payload -> {
                Route route = Route.getRootAsRoute(payload.getMetadata());
                return route.destNamespace();
              })
          .flatMap(
              grouped -> {
                Handler handler;
                synchronized (ServiceHandler.this) {
                  handler = handlers.get(grouped.key());
                }

                if (handler == null) {
                  throw new IllegalStateException(
                      "no handler found for namespace with id " + grouped.key());
                }

                return handler.requestChannel(grouped);
              });
    } catch (Throwable t) {
      return Flux.error(t);
    }
  }

  @Override
  public Mono<Void> metadataPush(RawPayload payload) {
    try {
      return findHandler(payload).metadataPush(payload);
    } catch (Throwable t) {
      return Mono.error(t);
    }
  }
}
