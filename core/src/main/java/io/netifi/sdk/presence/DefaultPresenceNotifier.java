package io.netifi.sdk.presence;

import io.netifi.sdk.frames.DestinationAvailResult;
import io.netifi.sdk.frames.RouteDestinationFlyweight;
import io.netifi.sdk.frames.RouteType;
import io.netifi.sdk.frames.RoutingFlyweight;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.PayloadImpl;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ReplayProcessor;

/** Implementation of {@link PresenceNotifier} */
public class DefaultPresenceNotifier implements PresenceNotifier {
  private static final byte[] EMPTY = new byte[0];
  private static final Object EVENT = new Object();
  final Set<PresenceNotificationInfo> presenceInfo;
  final ConcurrentHashMap<String, Disposable> watchingSubscriptions;
  private final TimebasedIdGenerator idGenerator;
  private final ReplayProcessor onChange;
  private final long fromAccessKey;
  private final String fromDestination;
  private final RSocket reconnectingRSocket;

  public DefaultPresenceNotifier(
      TimebasedIdGenerator idGenerator,
      long fromAccessKey,
      String fromDestination,
      RSocket reconnectingRSocket) {
    this.onChange = ReplayProcessor.cacheLast();
    this.idGenerator = idGenerator;
    this.fromAccessKey = fromAccessKey;
    this.fromDestination = fromDestination;
    this.reconnectingRSocket = reconnectingRSocket;
    this.presenceInfo = ConcurrentHashMap.newKeySet();
    this.watchingSubscriptions = new ConcurrentHashMap<>();

    reconnectingRSocket
        .onClose()
        .doFinally(
            s -> {
              watchingSubscriptions.values().forEach(Disposable::dispose);
              watchingSubscriptions.clear();
              presenceInfo.clear();
            })
        .subscribe();
  }

  public void watch(long accountId, String group) {
    watchingSubscriptions.computeIfAbsent(
        groupKey(accountId, group),
        k -> {
          ByteBuf route = createGroupRoute(RouteType.PRESENCE_GROUP_QUERY, accountId, group);
          byte[] routingInformation = createRoutingInformation(route, fromDestination);
          Payload payload = new PayloadImpl(EMPTY, routingInformation);

          Disposable subscribe =
              reconnectingRSocket
                  .requestStream(payload)
                  .doOnNext(
                      p -> {
                        ByteBuf metadata = Unpooled.wrappedBuffer(p.getMetadata());
                        boolean found = DestinationAvailResult.found(metadata);

                        PresenceNotificationInfo presenceNotificationInfo =
                            new PresenceNotificationInfo(null, accountId, group);
                        if (found) {
                          presenceInfo.add(presenceNotificationInfo);
                        } else {
                          presenceInfo.remove(presenceNotificationInfo);
                        }
                        onChange.onNext(EMPTY);
                      })
                  .doFinally(
                      s -> {
                        watchingSubscriptions.remove(k);
                      })
                  .subscribe();

          return subscribe;
        });
  }

  public void stopWatching(long accountId, String group) {
    Disposable disposable = watchingSubscriptions.get(groupKey(accountId, group));
    if (disposable != null) {
      disposable.dispose();
    }
  }

  public void watch(long accountId, String destination, String group) {
    watchingSubscriptions.computeIfAbsent(
        groupKey(accountId, group),
        k -> {
          ByteBuf route =
              createDestinationRoute(RouteType.PRESENCE_ID_QUERY, accountId, destination, group);
          byte[] routingInformation = createRoutingInformation(route, fromDestination);
          Payload payload = new PayloadImpl(EMPTY, routingInformation);

          Disposable subscribe =
              reconnectingRSocket
                  .requestStream(payload)
                  .doOnNext(
                      p -> {
                        ByteBuf metadata = Unpooled.wrappedBuffer(p.getMetadata());
                        boolean found = DestinationAvailResult.found(metadata);

                        PresenceNotificationInfo presenceNotificationInfo =
                            new PresenceNotificationInfo(destination, accountId, group);
                        if (found) {
                          presenceInfo.add(presenceNotificationInfo);
                        } else {
                          presenceInfo.remove(presenceNotificationInfo);
                        }
                        onChange.onNext(EVENT);
                      })
                  .doFinally(
                      s -> {
                        watchingSubscriptions.remove(k);
                      })
                  .subscribe();

          return subscribe;
        });
  }

  public void stopWatching(long accountId, String destination, String group) {
    Disposable disposable =
        watchingSubscriptions.get(destinationkey(accountId, destination, group));
    if (disposable != null) {
      disposable.dispose();
    }
  }

  @SuppressWarnings("unchecked")
  public Mono<Void> notify(long accountId, String group) {
    if (!watchingSubscriptions.contains(groupKey(accountId, group))) {
      watch(accountId, group);
    }

    PresenceNotificationInfo info = new PresenceNotificationInfo(null, accountId, group);
    if (presenceInfo.contains(info)) {
      return Mono.empty();
    } else {
      return onChange.filter(o -> presenceInfo.contains(info)).next().then();
    }
  }

  @SuppressWarnings("unchecked")
  public Mono<Void> notify(long accountId, String destination, String group) {
    if (!watchingSubscriptions.contains(destinationkey(accountId, destination, group))) {
      watch(accountId, destination, group);
    }

    PresenceNotificationInfo info = new PresenceNotificationInfo(destination, accountId, group);
    if (presenceInfo.contains(info)) {
      return Mono.empty();
    } else {
      return onChange.filter(o -> presenceInfo.contains(info)).next().then();
    }
  }

  private String groupKey(long accountId, String group) {
    return accountId + ":" + group;
  }

  private String destinationkey(long accountId, String destination, String group) {
    return accountId + ":" + destination + ":" + group;
  }

  private ByteBuf createGroupRoute(RouteType routeType, long accountId, String group) {
    int length = RouteDestinationFlyweight.computeLength(routeType, group);
    byte[] bytes = new byte[length];
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    RouteDestinationFlyweight.encodeRouteByGroup(byteBuf, routeType, accountId, group);

    return byteBuf;
  }

  private ByteBuf createDestinationRoute(
      RouteType routeType, long accountId, String destination, String group) {
    int length = RouteDestinationFlyweight.computeLength(routeType, destination, group);
    byte[] bytes = new byte[length];
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    RouteDestinationFlyweight.encodeRouteByDestination(
        byteBuf, routeType, accountId, destination, group);

    return byteBuf;
  }

  private byte[] createRoutingInformation(ByteBuf route, String fromDestination) {
    int length = RoutingFlyweight.computeLength(false, fromDestination, route);
    byte[] bytes = new byte[length];
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);

    RoutingFlyweight.encode(
        byteBuf, false, 1, fromAccessKey, fromDestination, idGenerator.nextId(), route);

    return bytes;
  }

  class PresenceNotificationInfo
      implements DestinationPresenceNotificationInfo, GroupPresenceNotificationInfo {
    private String destination;
    private long accountId;
    private String group;

    PresenceNotificationInfo(String destination, long accountId, String group) {
      this.destination = destination;
      this.accountId = accountId;
      this.group = group;
    }

    public String getDestination() {
      return destination;
    }

    public long getAccountId() {
      return accountId;
    }

    public String getGroup() {
      return group;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PresenceNotificationInfo that = (PresenceNotificationInfo) o;

      if (accountId != that.accountId) return false;
      if (destination != null ? !destination.equals(that.destination) : that.destination != null)
        return false;
      return group != null ? group.equals(that.group) : that.group == null;
    }

    @Override
    public int hashCode() {
      int result = destination != null ? destination.hashCode() : 0;
      result = 31 * result + (int) (accountId ^ (accountId >>> 32));
      result = 31 * result + (group != null ? group.hashCode() : 0);
      return result;
    }
  }
}
