package io.netifi.sdk.rs;

import io.netifi.proteus.frames.RoutingFlyweight;
import io.netifi.sdk.RouteNotFoundException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.lang.reflect.Field;

public class RequestHandlingRSocket implements RSocket {
  private static final String PACKAGE_ID = "PACKAGE_ID";
  private static final String SERVICE_ID = "SERVICE_ID";
  private final IntObjectMap<IntObjectMap<RSocket>> registeredServices;
  private MonoProcessor<Void> onClose;

  public RequestHandlingRSocket(RSocket... rSockets) {
    this.onClose = MonoProcessor.create();
    this.registeredServices = new IntObjectHashMap<>();

    for (RSocket rSocket : rSockets) {
      try {
        Class<? extends RSocket> clazz = rSocket.getClass();
        Field packageIdField = clazz.getDeclaredField(PACKAGE_ID);
        Field serviceIdField = clazz.getDeclaredField(SERVICE_ID);

        packageIdField.setAccessible(true);
        serviceIdField.setAccessible(true);

        int packageId = packageIdField.getInt(rSocket);
        int serviceId = serviceIdField.getInt(rSocket);

        IntObjectMap<RSocket> packageMap = registeredServices.computeIfAbsent(packageId, integer -> new IntObjectHashMap<>());
        packageMap.put(serviceId, rSocket);
      } catch (Exception t) {
        throw new RuntimeException(t);
      }
    }
  }

  @Override
  public Mono<Void> fireAndForget(Payload payload) {
    try {
      ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
      int packageId = RoutingFlyweight.namespaceId(metadata);
      int serviceId = RoutingFlyweight.serviceId(metadata);

      IntObjectMap<RSocket> rSocketIntObjectMap = registeredServices.get(packageId);

      if (rSocketIntObjectMap == null) {
        return Mono.error(new RouteNotFoundException(packageId));
      }

      RSocket rSocket = rSocketIntObjectMap.get(serviceId);

      if (rSocket == null) {
        return Mono.error(new RouteNotFoundException(packageId, serviceId));
      }

      return rSocket.fireAndForget(payload);

    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Mono<Payload> requestResponse(Payload payload) {
    try {
      ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
      int packageId = RoutingFlyweight.namespaceId(metadata);
      int serviceId = RoutingFlyweight.serviceId(metadata);

      IntObjectMap<RSocket> rSocketIntObjectMap = registeredServices.get(packageId);

      if (rSocketIntObjectMap == null) {
        return Mono.error(new RouteNotFoundException(packageId));
      }

      RSocket rSocket = rSocketIntObjectMap.get(serviceId);

      if (rSocket == null) {
        return Mono.error(new RouteNotFoundException(packageId, serviceId));
      }

      return rSocket.requestResponse(payload);

    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Flux<Payload> requestStream(Payload payload) {
    try {
      ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
      int packageId = RoutingFlyweight.namespaceId(metadata);
      int serviceId = RoutingFlyweight.serviceId(metadata);

      IntObjectMap<RSocket> rSocketIntObjectMap = registeredServices.get(packageId);

      if (rSocketIntObjectMap == null) {
        return Flux.error(new RouteNotFoundException(packageId));
      }

      RSocket rSocket = rSocketIntObjectMap.get(serviceId);

      if (rSocket == null) {
        return Flux.error(new RouteNotFoundException(packageId, serviceId));
      }

      return rSocket.requestStream(payload);

    } catch (Throwable t) {
      return Flux.error(t);
    }
  }

  @Override
  public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
    return Flux.from(payloads)
        .groupBy(
            payload -> {
              ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
              int packageId = RoutingFlyweight.namespaceId(metadata);
              int serviceId = RoutingFlyweight.serviceId(metadata);

              IntObjectMap<RSocket> rSocketIntObjectMap = registeredServices.get(packageId);

              if (rSocketIntObjectMap == null) {
                return Flux.error(new RouteNotFoundException(packageId));
              }

              RSocket rSocket = rSocketIntObjectMap.get(serviceId);

              return new Key(packageId, serviceId, rSocket);
            })
        .flatMap(
            groupedPayloads -> {
              Key key = (Key) groupedPayloads.key();
              return key.rSocket.requestChannel(groupedPayloads);
            });
  }

  @Override
  public Mono<Void> metadataPush(Payload payload) {
    try {
      ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
      int packageId = RoutingFlyweight.namespaceId(metadata);
      int serviceId = RoutingFlyweight.serviceId(metadata);

      IntObjectMap<RSocket> rSocketIntObjectMap = registeredServices.get(packageId);

      if (rSocketIntObjectMap == null) {
        return Mono.error(new RouteNotFoundException(packageId));
      }

      RSocket rSocket = rSocketIntObjectMap.get(serviceId);

      if (rSocket == null) {
        return Mono.error(new RouteNotFoundException(packageId, serviceId));
      }

      return rSocket.metadataPush(payload);

    } catch (Throwable t) {
      return Mono.error(t);
    }
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }

  @Override
  public Mono<Void> close() {
    return Mono.fromRunnable(() -> onClose.onComplete())
        .doFinally(
            s -> {
              registeredServices.forEach(
                  (integer, rSocketIntObjectMap) -> {
                    rSocketIntObjectMap.forEach(
                        (integer1, socket) -> {
                          socket.close().subscribe();
                        });
                  });
            })
        .then();
  }

  @Override
  public double availability() {
    return 1.0;
  }

  private class Key {
    int packageId;
    int serviceId;

    RSocket rSocket;

    public Key(int packageId, int serviceId, RSocket rSocket) {
      this.packageId = packageId;
      this.serviceId = serviceId;
      this.rSocket = rSocket;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Key key = (Key) o;

      if (packageId != key.packageId) return false;
      return serviceId == key.serviceId;
    }

    @Override
    public int hashCode() {
      int result = packageId;
      result = 31 * result + serviceId;
      return result;
    }
  }
}
