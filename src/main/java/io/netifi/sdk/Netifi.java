package io.netifi.sdk;

import io.netifi.nrqp.frames.DestinationSetupFlyweight;
import io.netifi.sdk.rs.DefaultNetifiSocket;
import io.netifi.sdk.rs.NetifiSocket;
import io.netifi.sdk.rs.ReconnectingRSocket;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.RSocket;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.PayloadImpl;
import java.util.Collection;
import java.util.Objects;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** This is where the magic happens */
public class Netifi implements PresenceNotificationHandler {
  private static final Logger logger = LoggerFactory.getLogger(Netifi.class);

  static {
    // Set the Java DNS cache to 60 seconds
    java.security.Security.setProperty("networkaddress.cache.ttl", "60");
  }

  private final TimebasedIdGenerator idGenerator;
  private final PresenceNotificationHandler presenceNotificationHandler;
  private final long fromAccountId;
  private final String fromDestination;
  private final String fromGroup;
  private final ReconnectingRSocket reconnectingRSocket;
  private final long accessKey;
  private final byte[] accessTokenBytes;
  private final boolean keepalive;
  private volatile boolean running = true;

  private Netifi(
      String host,
      int port,
      long accessKey,
      long fromAccountId,
      String destination,
      String group,
      byte[] accessTokenBytes,
      boolean keepalive,
      long tickPeriodSeconds,
      long ackTimeoutSeconds,
      int missedAcks,
      RSocket requestHandlingRSocket) {
    this.keepalive = keepalive;
    this.accessKey = accessKey;
    this.fromAccountId = fromAccountId;
    this.fromDestination = destination;
    this.fromGroup = group;
    this.idGenerator = new TimebasedIdGenerator(destination.hashCode());
    this.presenceNotificationHandler = null;
    this.accessTokenBytes = accessTokenBytes;
    //        new DefaultPresenceNotificationHandler(
    //            barrier, () -> running, idGenerator, fromAccountId, destination);

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

    Objects.nonNull(requestHandlingRSocket);

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
    // return presenceNotificationHandler.presence(fromAccountId, group);
    return null;
  }

  @Override
  public Flux<Collection<String>> presence(long accountId, String group, String destination) {
    // return presenceNotificationHandler.presence(fromAccountId, group, destination);
    return null;
  }

  public Mono<NetifiSocket> connect(String group, String destination) {
    return Mono.just(
        new DefaultNetifiSocket(
            reconnectingRSocket,
            accessKey,
            fromAccountId,
            fromDestination,
            destination,
            group,
            accessTokenBytes,
            keepalive,
            idGenerator));
  }

  public Mono<NetifiSocket> connect(String group) {
    return connect(group, null);
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
    private RSocket requestHandler;

    private Builder() {}

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

    public Builder addHandler(RSocket requestHandler) {
      this.requestHandler = requestHandler;
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
          requestHandler);
    }
  }
}
