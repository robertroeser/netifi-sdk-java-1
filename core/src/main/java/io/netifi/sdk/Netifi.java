package io.netifi.sdk;

import io.netifi.proteus.ProteusService;
import io.netifi.proteus.rs.RequestHandlingRSocket;
import io.netifi.sdk.frames.DestinationSetupFlyweight;
import io.netifi.sdk.presence.DefaultPresenceNotifier;
import io.netifi.sdk.presence.PresenceNotifier;
import io.netifi.sdk.rs.*;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Closeable;
import io.rsocket.RSocket;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.PayloadImpl;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

/** This is where the magic happens */
public class Netifi implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(Netifi.class);
  private static final ConcurrentHashMap<String, Netifi> NETIFI = new ConcurrentHashMap<>();

  static {
    // Set the Java DNS cache to 60 seconds
    java.security.Security.setProperty("networkaddress.cache.ttl", "60");
  }

  private final TimebasedIdGenerator idGenerator;
  private final PresenceNotifier presenceNotifier;
  private final long fromAccountId;
  private final String fromDestination;
  private final String fromGroup;
  private final ReconnectingRSocket reconnectingRSocket;
  private final long accessKey;
  private final byte[] accessTokenBytes;
  private final boolean keepalive;
  private volatile boolean running = true;
  private MonoProcessor<Void> onClose;
  private RequestHandlingRSocket requestHandlingRSocket;

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
      int missedAcks) {
    this.onClose = MonoProcessor.create();
    this.keepalive = keepalive;
    this.accessKey = accessKey;
    this.fromAccountId = fromAccountId;
    this.fromDestination = destination;
    this.fromGroup = group;
    this.idGenerator = new TimebasedIdGenerator(destination.hashCode());

    this.accessTokenBytes = accessTokenBytes;
    //        new DefaultPresenceNotifier(
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

    requestHandlingRSocket = new RequestHandlingRSocket();

    this.reconnectingRSocket =
        new ReconnectingRSocket(
            MetadataUnwrappingRSocket.wrap(requestHandlingRSocket),
            () -> new PayloadImpl(empty, metadata),
            () -> running,
            () -> TcpClientTransport.create(host, port),
            keepalive,
            tickPeriodSeconds,
            ackTimeoutSeconds,
            missedAcks,
            accessKey,
            accessTokenBytes);

    this.presenceNotifier =
        new DefaultPresenceNotifier(idGenerator, accessKey, fromDestination, reconnectingRSocket);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Mono<Void> close() {
    return Mono.fromRunnable(onClose::onComplete)
        .doFinally(
            s -> {
              running = false;
              requestHandlingRSocket.close().subscribe();
              reconnectingRSocket.close().subscribe();
            })
        .then();
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }

  public Netifi addService(ProteusService service) {
    requestHandlingRSocket.addService(service);
    return this;
  }

  public Mono<NetifiSocket> connect(String group, String destination) {
    return Mono.just(
        PresenceAwareRSocket.wrap(
            new DefaultNetifiSocket(
                reconnectingRSocket,
                accessKey,
                fromAccountId,
                fromDestination,
                destination,
                group,
                accessTokenBytes,
                keepalive,
                idGenerator),
            fromAccountId,
            destination,
            group,
            presenceNotifier));
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
    private boolean keepalive = false;
    private long tickPeriodSeconds = 60;
    private long ackTimeoutSeconds = 120;
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

      String netifiKey = accessKey + accountId + group + destination;

      return NETIFI.computeIfAbsent(
          netifiKey,
          _k -> {
            Netifi netifi =
                new Netifi(
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
                    missedAcks);
            netifi.onClose.doFinally(s -> NETIFI.remove(netifiKey)).subscribe();
            return netifi;
          });
    }
  }
}
