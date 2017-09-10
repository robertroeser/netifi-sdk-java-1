package io.netifi.sdk;

import io.netifi.nrqp.frames.DestinationSetupFlyweight;
import io.netifi.sdk.annotations.Service;
import io.netifi.sdk.rs.RequestHandlingRSocket;
import io.netifi.sdk.util.HashUtil;
import io.netifi.sdk.util.RSocketBarrier;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.PayloadImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.xml.bind.DatatypeConverter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static io.netifi.sdk.util.HashUtil.hash;

/** This is where the magic happens */
public class Netifi implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(Netifi.class);
  private static final Throwable CONNECTION_CLOSED = new Throwable("connection is closed");
  private final TimebasedIdGenerator idGenerator;
  private final String host;
  private final int port;
  private final long accessKey;
  private final long accountId;
  private final long[] groupIds;
  private final String destination;
  private final long destinationId;
  private final String group;
  private final String accessToken;
  private final byte[] accessTokenBytes;
  private final RequestHandlerRegistry registry;
  private final RSocketBarrier barrier;
  private final Disposable disposable;
  private volatile boolean running = true;

  private Netifi(
      String host,
      int port,
      long accessKey,
      long accountId,
      long[] groupIds,
      String destination,
      long destinationId,
      String group,
      String accessToken,
      byte[] accessTokenBytes) {
    this.barrier = new RSocketBarrier();
    this.registry = new DefaultRequestHandlerRegistry();
    this.host = host;
    this.port = port;
    this.accessKey = accessKey;
    this.accountId = accountId;
    this.groupIds = groupIds;
    this.destination = destination;
    this.destinationId = destinationId;
    this.group = group;
    this.accessToken = accessToken;
    this.accessTokenBytes = accessTokenBytes;
    this.idGenerator = new TimebasedIdGenerator((int) destinationId);

    AtomicLong delay = new AtomicLong();
    this.disposable =
        Mono.create(
                sink -> {
                  int length = DestinationSetupFlyweight.computeLength(false, groupIds.length);
                  byte[] bytes = new byte[length];
                  ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
                  DestinationSetupFlyweight.encode(
                      byteBuf,
                      Unpooled.EMPTY_BUFFER,
                      Unpooled.wrappedBuffer(new byte[20]),
                      accountId,
                      destinationId,
                      idGenerator.nextId(),
                      groupIds);

                  RSocketFactory.connect()
                      //.keepAlive(Duration.ofSeconds(1), Duration.ofSeconds(5), 3)
                      .errorConsumer(throwable -> logger.error("unhandled error", throwable))
                      .setupPayload(new PayloadImpl(new byte[0], bytes))
                      .acceptor(
                          rSocket -> {
                            rSocket
                                .onClose()
                                .doFinally(
                                    s -> {
                                      if (running) {
                                        sink.success();
                                      } else {
                                        sink.error(CONNECTION_CLOSED);
                                      }
                                    })
                                .subscribe();
                            barrier.setRSocket(rSocket);
                            logger.info(
                                "destination with id "
                                    + destinationId
                                    + " connected to "
                                    + host
                                    + ":"
                                    + port);

                            return new RequestHandlingRSocket(registry);
                          })
                      .transport(TcpClientTransport.create(host, port))
                      .start()
                      .timeout(Duration.ofSeconds(30))
                      .doOnSuccess(s -> delay.set(0))
                      .doOnError(sink::error)
                      .subscribe();
                })
            .doOnError(Throwable::printStackTrace)
            .onErrorResume(
                t -> {
                  long d = Math.min(10_000, delay.addAndGet(500));
                  return Mono.delay(Duration.ofMillis(d)).then(Mono.error(t));
                })
            .retry(throwable -> running)
            .subscribe();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    return "Service{"
        + "host='"
        + host
        + '\''
        + ", port="
        + port
        + ", accessKey="
        + accessKey
        + ", accountId="
        + accountId
        + ", groupIds="
        + Arrays.toString(groupIds)
        + ", destination='"
        + destination
        + '\''
        + ", destinationId="
        + destinationId
        + ", group='"
        + group
        + '\''
        + ", accessToken='"
        + accessToken
        + '\''
        + ", accessTokenBytes="
        + Arrays.toString(accessTokenBytes)
        + ", running="
        + running
        + '}';
  }

  public <T> T create(Class<T> service) {
    return create(service, -1);
  }

  public <T> T create(Class<T> service, String destination) {
    return create(service, HashUtil.hash(destination));
  }

  public <T> T create(Class<T> service, long destinationId) {
    Objects.requireNonNull(service, "service must be non-null");
    Annotation[] annotations = service.getDeclaredAnnotations();

    if (annotations == null || annotations.length == 0) {
      throw new IllegalStateException("the class " + service.getName() + " has no annotations");
    }

    Service Service = null;
    for (Annotation annotation : annotations) {
      if (annotation instanceof Service) {
        Service = (Service) annotation;
        break;
      }
    }

    Objects.requireNonNull(Service, "no Service annotation found on " + service.getName());

    long accountId = Service.accountId();
    String group = Service.group();

    return create(service, accountId, group, destinationId);
  }

  public <T> T create(Class<T> service, long accountId, String group) {
    return create(service, accountId, group, -1);
  }

  public <T> T create(Class<T> service, long accountId, String group, String destination) {
    return create(service, accountId, group, HashUtil.hash(destination));
  }

  public <T> T create(Class<T> service, long accountId, String group, long destinationId) {
    Objects.requireNonNull(service, "service must be non-null");

    logger.info("creating service {}, {}, {}, {}", service, accountId, group, destinationId);

    Object o =
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {service},
            new NetifiInvocationHandler(
                barrier,
                accountId,
                group,
                destinationId,
                this.accountId,
                this.groupIds,
                this.destinationId,
                idGenerator));

    return (T) o;
  }

  public <T1, T2> void registerHandler(Class<T1> clazz, T2 t) {
    logger.info(
        "registering class {}, with interface {}", t.getClass().toString(), clazz.toString());
    registry.registerHandler(clazz, t);
  }

  @Override
  public void close() throws Exception {
    if (disposable != null) {
      disposable.dispose();
    }
    running = false;
  }

  public static class Builder {
    private String host = "netifiedge.trafficmanager.net";
    private Integer port = 8001;
    private Long accessKey;
    private Long accountId;
    private String group;
    private long[] groupIds;
    private String destination;
    private Long destinationId;
    private String accessToken = null;
    private byte[] accessTokenBytes = new byte[20];

    private Builder() {}

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
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
      String[] split = group.split("\\.");
      this.groupIds = new long[split.length];

      for (int i = 0; i < split.length; i++) {
        groupIds[i] = Math.abs(hash(split[i]));
      }

      return this;
    }

    public Builder accountId(long accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder destination(String destination) {
      this.destination = destination;
      this.destinationId = HashUtil.hash(destination);

      return this;
    }

    public Builder destinationId(long destinationId) {
      this.destinationId = destinationId;
      return this;
    }

    public Netifi build() {
      Objects.requireNonNull(host, "host is required");
      Objects.requireNonNull(port, "port is required");
      Objects.requireNonNull(accountId, "account Id is required");
      Objects.requireNonNull(group, "group is required");
      Objects.requireNonNull(destinationId, "destination id is required");

      logger.info(
          "registering with netifi with account id {}, group {}, groupIds {}, destination {}, and destination id {}",
          accountId,
          group,
          groupIds,
          destination,
          destinationId);

      return new Netifi(
          host,
          port,
          accessKey == null ? 0 : accessKey,
          accountId,
          groupIds,
          destination,
          destinationId,
          group,
          accessToken,
          accessTokenBytes);
    }
  }
}
