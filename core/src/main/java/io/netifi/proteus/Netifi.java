package io.netifi.proteus;

import io.netifi.proteus.balancer.LoadBalancedRSocketSupplier;
import io.netifi.proteus.balancer.transport.ClientTransportSupplierFactory;
import io.netifi.proteus.connection.DestinationNameFactory;
import io.netifi.proteus.connection.SocketAddressFactory;
import io.netifi.proteus.frames.DestinationSetupFlyweight;
import io.netifi.proteus.presence.DefaultPresenceNotifier;
import io.netifi.proteus.presence.PresenceNotifier;
import io.netifi.proteus.rs.DefaultNetifiSocket;
import io.netifi.proteus.rs.MetadataUnwrappingRSocket;
import io.netifi.proteus.rs.NetifiSocket;
import io.netifi.proteus.rs.PresenceAwareRSocket;
import io.netifi.proteus.rs.RequestHandlingRSocket;
import io.netifi.proteus.rs.WeightedReconnectingRSocket;
import io.netifi.proteus.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.nio.NioEventLoopGroup;
import io.rsocket.Closeable;
import io.rsocket.Payload;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.ipc.netty.tcp.TcpClient;

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
  private final long accessKey;
  private final byte[] accessTokenBytes;
  private final boolean keepalive;
  private final LoadBalancedRSocketSupplier rSocketSupplier;
  private final ClientTransportSupplierFactory transportSupplierFactory;
  private volatile boolean running = true;
  private MonoProcessor<Void> onClose;
  private RequestHandlingRSocket requestHandlingRSocket;
  private Executor executor;

  private Netifi(
      long accessKey,
      long fromAccountId,
      String group,
      byte[] accessTokenBytes,
      boolean keepalive,
      long tickPeriodSeconds,
      long ackTimeoutSeconds,
      int missedAcks,
      int poolSize,
      DestinationNameFactory destinationNameFactory,
      SocketAddressFactory socketAddressFactory,
      Executor executor) {
    this.onClose = MonoProcessor.create();
    this.keepalive = keepalive;
    this.accessKey = accessKey;
    this.fromAccountId = fromAccountId;
    this.fromDestination = destinationNameFactory.peek();
    this.fromGroup = group;
    this.idGenerator = new TimebasedIdGenerator(ThreadLocalRandom.current().nextInt());
    this.executor = executor;
    this.accessTokenBytes = accessTokenBytes;
    this.requestHandlingRSocket = new RequestHandlingRSocket();
    this.transportSupplierFactory =
        new ClientTransportSupplierFactory(socketAddressFactory.get(), this::createClientTransport);

    final Function<String, Payload> setupPayloadSupplier =
        d -> {
          logger.debug("creating a connection with group {} and destination {}", group, d);

          int length = DestinationSetupFlyweight.computeLength(false, d, group);

          ByteBuf metadata = ByteBufAllocator.DEFAULT.directBuffer(length);
          DestinationSetupFlyweight.encode(
              metadata,
              Unpooled.EMPTY_BUFFER,
              Unpooled.wrappedBuffer(accessTokenBytes),
              idGenerator.nextId(),
              accessKey,
              d,
              group);
          return ByteBufPayload.create(Unpooled.EMPTY_BUFFER, metadata);
        };

    this.rSocketSupplier =
        new LoadBalancedRSocketSupplier(
            poolSize,
            (lowerQuantile, higherQuantile) -> {
              return new WeightedReconnectingRSocket(
                  MetadataUnwrappingRSocket.wrap(requestHandlingRSocket),
                  destinationNameFactory,
                  setupPayloadSupplier,
                  () -> running,
                  transportSupplierFactory,
                  keepalive,
                  tickPeriodSeconds,
                  ackTimeoutSeconds,
                  missedAcks,
                  accessKey,
                  accessTokenBytes,
                  lowerQuantile,
                  higherQuantile,
                  500);
            });

    this.presenceNotifier =
        new DefaultPresenceNotifier(idGenerator, accessKey, fromDestination, rSocketSupplier);
  }

  public static Builder builder() {
    return new Builder();
  }

  private Supplier<ClientTransport> createClientTransport(SocketAddress address) {
    return () -> {
      InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
      if (executor == null) {
        return TcpClientTransport.create(inetSocketAddress);
      } else {
        NioEventLoopGroup group =
            new NioEventLoopGroup(
                Runtime.getRuntime().availableProcessors(), ForkJoinPool.commonPool());
        TcpClient client =
            TcpClient.builder()
                .options(
                    options -> {
                      options.disablePool();
                      options.eventLoopGroup(group);
                      options.connectAddress(() -> address);
                    })
                .build();

        return TcpClientTransport.create(client);
      }
    };
  }

  @Override
  public Mono<Void> close() {
    return Mono.fromRunnable(onClose::onComplete)
        .doOnSubscribe(s -> running = false)
        .then(requestHandlingRSocket.close())
        .then(transportSupplierFactory.close())
        .then(rSocketSupplier.close());
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
                rSocketSupplier,
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
    private String host;
    private Integer port = 8001;
    private List<SocketAddress> addresses;
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
    private int poolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
    private DestinationNameFactory destinationNameFactory;
    private SocketAddressFactory socketAddressFactory;

    private Executor executor = null;

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

    public Builder socketAddress(Collection<SocketAddress> addresses) {
      if (addresses instanceof List) {
        this.addresses = (List<SocketAddress>) addresses;
      } else {
        List<SocketAddress> list = new ArrayList<>();
        list.addAll(addresses);
        this.addresses = list;
      }

      return this;
    }

    public Builder socketAddress(SocketAddress address, SocketAddress... addresses) {
      List<SocketAddress> list = new ArrayList<>();
      list.add(address);

      if (addresses != null) {
        list.addAll(Arrays.asList(addresses));
      }

      return socketAddress(list);
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
      this.accessTokenBytes = Base64.getDecoder().decode(accessToken);
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

    public Builder executor(Executor executor) {
      this.executor = executor;
      return this;
    }

    public Builder poolSize(int poolSize) {
      this.poolSize = poolSize;

      return this;
    }

    public Builder destinationNameFactory(DestinationNameFactory destinationNameFactory) {
      this.destinationNameFactory = destinationNameFactory;
      return this;
    }

    public Builder socketAddressFactory(SocketAddressFactory socketAddressFactory) {
      this.socketAddressFactory = socketAddressFactory;
      return this;
    }

    public Netifi build() {
      Objects.requireNonNull(accessKey, "account key is required");
      Objects.requireNonNull(accessToken, "account token is required");
      Objects.requireNonNull(accountId, "account Id is required");
      Objects.requireNonNull(group, "group is required");

      if (poolSize < 1) {
        throw new IllegalStateException("poolSize must be greater the 0");
      }

      if (destinationNameFactory == null) {
        if (destination == null) {
          destination = UUID.randomUUID().toString();
        }

        if (poolSize == 1) {
          destinationNameFactory = DestinationNameFactory.from(destination);
        } else {
          destinationNameFactory = DestinationNameFactory.from(destination, new AtomicInteger());
        }
      }

      if (socketAddressFactory == null) {
        if (addresses == null) {
          Objects.requireNonNull(host, "host is required");
          Objects.requireNonNull(port, "port is required");
          socketAddressFactory = SocketAddressFactory.from(host, port);
        } else {
          socketAddressFactory = SocketAddressFactory.from(addresses);
        }
      }

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
                    accessKey,
                    accountId,
                    group,
                    accessTokenBytes,
                    keepalive,
                    tickPeriodSeconds,
                    ackTimeoutSeconds,
                    missedAcks,
                    poolSize,
                    destinationNameFactory,
                    socketAddressFactory,
                    executor);
            netifi.onClose.doFinally(s -> NETIFI.remove(netifiKey)).subscribe();
            return netifi;
          });
    }
  }
}
