package io.netifi.sdk;

import io.netifi.nrqp.frames.DestinationAvailResult;
import io.netifi.nrqp.frames.RouteDestinationFlyweight;
import io.netifi.nrqp.frames.RouteType;
import io.netifi.nrqp.frames.RoutingFlyweight;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.BooleanSupplier;
import io.reactivex.processors.RSocketBarrier;
import io.rsocket.Payload;
import io.rsocket.util.PayloadImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Implementation of {@link PresenceNotificationHandler} */
public class DefaultPresenceNotificationHandler implements PresenceNotificationHandler {
  private static final Logger logger = LoggerFactory.getLogger(Netifi.class);

  private final RSocketBarrier barrier;

  private final BooleanSupplier running;

  private final TimebasedIdGenerator idGenerator;

  private final long accountId;

  private final String fromDestination;

  public DefaultPresenceNotificationHandler(
      RSocketBarrier barrier,
      BooleanSupplier running,
      TimebasedIdGenerator idGenerator,
      long accountId,
      String fromDestination) {
    this.barrier = barrier;
    this.running = running;
    this.idGenerator = idGenerator;
    this.accountId = accountId;
    this.fromDestination = fromDestination;
  }

  public Flowable<Collection<String>> presence(long accountId, String group) {
    return presence(accountId, group, null);
  }

  public Flowable<Collection<String>> presence(long accountId, String group, String destination) {
    try {
      ConcurrentSkipListSet<String> present = new ConcurrentSkipListSet<>();
      AtomicLong delay = new AtomicLong();
      return Flowable.<Collection<String>>create(
              source -> {
                io.reactivex.disposables.Disposable d =
                    barrier
                        .getRSocket()
                        .doOnSubscribe(
                            s -> {
                              if (logger.isDebugEnabled()) {
                                logger.debug(
                                    "streaming presence notification accountId {}, group {}, destination {}",
                                    accountId,
                                    group,
                                    destination);
                              }

                              if (!present.isEmpty()) {
                                present.clear();
                              }
                            })
                        .flatMap(
                            rSocket -> {
                              rSocket
                                  .onClose()
                                  .doFinally(s -> source.onError(Netifi.CONNECTION_CLOSED))
                                  .subscribe();

                              Payload payload;

                              if (destination != null && !destination.equals("")) {
                                int length =
                                    RouteDestinationFlyweight.computeLength(
                                        RouteType.PRESENCE_ID_QUERY, destination, group);
                                byte[] routeBytes = new byte[length];
                                ByteBuf route = Unpooled.wrappedBuffer(routeBytes);
                                RouteDestinationFlyweight.encodeRouteByDestination(
                                    route,
                                    RouteType.PRESENCE_ID_QUERY,
                                    accountId,
                                    destination,
                                    group);

                                length =
                                    RoutingFlyweight.computeLength(
                                        false, false, false, fromDestination, route);
                                byte[] metadataBytes = new byte[length];
                                ByteBuf metadata = Unpooled.wrappedBuffer(metadataBytes);
                                RoutingFlyweight.encode(
                                    metadata,
                                    false,
                                    false,
                                    false,
                                    0,
                                    this.accountId,
                                    fromDestination,
                                    0,
                                    0,
                                    0,
                                    0,
                                    idGenerator.nextId(),
                                    route);
                                payload = new PayloadImpl(new byte[0], metadataBytes);
                              } else {
                                int length =
                                    RouteDestinationFlyweight.computeLength(
                                        RouteType.PRESENCE_GROUP_QUERY, group);
                                byte[] routeBytes = new byte[length];
                                ByteBuf route = Unpooled.wrappedBuffer(routeBytes);
                                RouteDestinationFlyweight.encodeRouteByGroup(
                                    route, RouteType.PRESENCE_GROUP_QUERY, accountId, group);

                                length =
                                    RoutingFlyweight.computeLength(
                                        false, false, false, fromDestination, route);
                                byte[] metadataBytes = new byte[length];
                                ByteBuf metadata = Unpooled.wrappedBuffer(metadataBytes);
                                RoutingFlyweight.encode(
                                    metadata,
                                    false,
                                    false,
                                    false,
                                    0,
                                    this.accountId,
                                    fromDestination,
                                    0,
                                    0,
                                    0,
                                    0,
                                    idGenerator.nextId(),
                                    route);
                                payload = new PayloadImpl(new byte[0], metadataBytes);
                              }

                              return rSocket.requestStream(payload);
                            })
                        .doOnNext(
                            payload -> {
                              ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
                              boolean found = DestinationAvailResult.found(metadata);
                              String destinationFromStream =
                                  DestinationAvailResult.destination(metadata);
                              if (found) {
                                present.add(destinationFromStream);
                              } else {
                                present.remove(destinationFromStream);
                              }

                              source.onNext(present);
                            })
                        .subscribe();

                source.setDisposable(d);
              },
              BackpressureStrategy.BUFFER)
          .debounce(200, TimeUnit.MILLISECONDS)
          .onErrorResumeNext(
              t -> {
                logger.debug("error getting notification events", t);
                long d = Math.min(10_000, delay.addAndGet(500));
                return Mono.delay(Duration.ofMillis(d)).then(Mono.error(t));
              })
          .retry(
              throwable -> {
                logger.debug("Netifi is still running, retrying presence notification");
                return running.getAsBoolean();
              });
    } catch (Throwable t) {
      logger.error("error stream presence events", t);
      return Flowable.error(t);
    }
  }
}
