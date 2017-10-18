package io.netifi.sdk;

import io.netifi.auth.SessionUtil;
import io.netifi.proteus.frames.DestinationSetupFlyweight;
import io.netifi.proteus.util.TimebasedIdGenerator;
import io.netifi.sdk.rs.ReconnectingRSocket;
import io.netifi.sdk.rs.RequestHandlingRSocket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.RSocket;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.PayloadImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** This is where the magic happens */
public class Netifi implements AutoCloseable, PresenceNotificationHandler {
  private static final Logger logger = LoggerFactory.getLogger(Netifi.class);

  static {
    // Set the Java DNS cache to 60 seconds
    java.security.Security.setProperty("networkaddress.cache.ttl", "60");
  }

  private final TimebasedIdGenerator idGenerator;
  private final PresenceNotificationHandler presenceNotificationHandler;
  private final long accountId;
  private final String destination;
  private final String group;
  private final ReconnectingRSocket reconnectingRSocket;
  private volatile boolean running = true;

  private Netifi(
      String host,
      int port,
      long accessKey,
      long accountId,
      String destination,
      String group,
      byte[] accessTokenBytes,
      boolean keepalive,
      long tickPeriodSeconds,
      long ackTimeoutSeconds,
      int missedAcks,
      RequestHandlingRSocket requestHandlingRSocket) {
    this.accountId = accountId;
    this.destination = destination;
    this.group = group;
    this.idGenerator = new TimebasedIdGenerator(destination.hashCode());
    this.presenceNotificationHandler = null;
    //        new DefaultPresenceNotificationHandler(
    //            barrier, () -> running, idGenerator, accountId, destination);

    int length = DestinationSetupFlyweight.computeLength(false, destination, group);
    byte[] metadata = new byte[length];

    ByteBuf byteBuf = Unpooled.wrappedBuffer(metadata);
    DestinationSetupFlyweight.encode(
        byteBuf,
        Unpooled.EMPTY_BUFFER,
        Unpooled.wrappedBuffer(accessTokenBytes),
        idGenerator.nextId(),
        accessKey,
        destination,
        group);
    byte[] empty = new byte[0];

    this.reconnectingRSocket =
        new ReconnectingRSocket(
            requestHandlingRSocket,
            () -> new PayloadImpl(empty, metadata),
            () -> running,
            () -> TcpClientTransport.create(host, port),
            keepalive,
            tickPeriodSeconds,
            ackTimeoutSeconds,
            missedAcks,
            accessKey,
            accessTokenBytes);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Flux<Collection<String>> presence(long accountId, String group) {
    // return presenceNotificationHandler.presence(accountId, group);
    return null;
  }

  @Override
  public Flux<Collection<String>> presence(long accountId, String group, String destination) {
    // return presenceNotificationHandler.presence(accountId, group, destination);
    return null;
  }

  public <T> T create(Class<T> service, String group) {
    return create(service, group, null);
  }

  public <T> T create(Class<T> service, String group, String destination) {
    Objects.requireNonNull(service, "service must be non-null");

    logger.info(
        "creating service {}, accountId {}, group {}, destination {}",
        service,
        accountId,
        group,
        destination);

    try {
      Constructor<T> constructor =
          service.getConstructor(
              RSocket.class, // io.rsocket.RSocket rSocket
              Long.class, // long accountId
              String.class, // String group
              String.class, // String destination
              Long.class, // long fromAccountId
              String.class, // String fromGroup
              String.class, // String fromDestination
              TimebasedIdGenerator.class, // io.netifi.proteus.util.TimebasedIdGenerator generator,
              SessionUtil.class, // io.netifi.proteus.auth.SessionUtil sessionUtil,
              AtomicLong.class, // java.util.concurrent.atomic.AtomicLong currentSessionCounter,
              AtomicReference
                  .class // java.util.concurrent.atomic.AtomicReference<byte[]> currentSessionToken
              );

      return constructor.newInstance(
          reconnectingRSocket,
          accountId,
          group,
          destination,
          accountId,
          this.group,
          this.destination,
          idGenerator,
          reconnectingRSocket.getSessionUtil(),
          reconnectingRSocket.getCurrentSessionCounter().get(),
          reconnectingRSocket.getCurrentSessionToken());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws Exception {
    running = false;
  }

  public static class Builder {
    private String host = "edge.prd.netifi.io";
    private Integer port = 8001;
    private Long accessKey;
    private Long accountId;
    private String group;
    private String destination;
    private String accessToken = null;
    private byte[] accessTokenBytes = new byte[20];
    private boolean keepalive = true;
    private long tickPeriodSeconds = 5;
    private long ackTimeoutSeconds = 10;
    private int missedAcks = 3;
    private List<RSocket> handlers;

    private Builder() {
      handlers = new ArrayList<>();
    }

    public Builder keepalive(boolean useKeepAlive) {
      this.keepalive = keepalive;
      return this;
    }

    public Builder tickPeriodSeconds(long tickPeriodSeconds) {
      this.tickPeriodSeconds = tickPeriodSeconds;
      return this;
    }

    public Builder ackTimeoutSeconds(long ackTimeoutSeconds) {
      this.ackTimeoutSeconds = ackTimeoutSeconds;
      return this;
    }

    public Builder missedAcks(int missedAcks) {
      this.missedAcks = missedAcks;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder accountId(long accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder accessKey(long accessKey) {
      this.accessKey = accessKey;
      return this;
    }

    public Builder accessToken(String accessToken) {
      this.accessToken = accessToken;
      this.accessTokenBytes = DatatypeConverter.parseBase64Binary(accessToken);
      return this;
    }

    public Builder group(String group) {
      this.group = group;
      return this;
    }

    public Builder destination(String destination) {
      this.destination = destination;

      return this;
    }

    public Builder addHandler(RSocket rSocket) {
      handlers.add(rSocket);

      return this;
    }

    public Builder addHandlers(RSocket... rSockets) {
      Arrays.stream(rSockets).forEach(this::addHandler);
      return this;
    }

    public Netifi build() {
      Objects.requireNonNull(host, "host is required");
      Objects.requireNonNull(port, "port is required");
      Objects.requireNonNull(accessKey, "account key is required");
      Objects.requireNonNull(accessToken, "account token is required");
      Objects.requireNonNull(accountId, "account Id is required");
      Objects.requireNonNull(group, "group is required");
      Objects.requireNonNull(destination, "destination id is required");

      RSocket[] rSockets = handlers.toArray(new RSocket[handlers.size()]);

      logger.info(
          "registering with netifi with account id {}, group {}, and destination {}",
          accountId,
          group,
          destination);

      return new Netifi(
          host,
          port,
          accessKey,
          accountId,
          destination,
          group,
          accessTokenBytes,
          keepalive,
          tickPeriodSeconds,
          ackTimeoutSeconds,
          missedAcks,
          new RequestHandlingRSocket(rSockets));
    }
  }
}
