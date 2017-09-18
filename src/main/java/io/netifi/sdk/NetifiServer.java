package io.netifi.sdk;

import io.netifi.sdk.annotations.Service;
import io.netifi.sdk.rs.LoadBalancedRSocketBarrier;
import io.netifi.sdk.rs.RSocketBarrier;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/** This is where the magic happens */
public class NetifiServer implements AutoCloseable, PresenceNotificationHandler {
  static final Throwable CONNECTION_CLOSED = new Throwable("connection is closed");
  private static final Logger logger = LoggerFactory.getLogger(NetifiServer.class);

  static {
    // Set the Java DNS cache to 60 seconds
    java.security.Security.setProperty("networkaddress.cache.ttl", "60");
  }

  private final TimebasedIdGenerator idGenerator;
  private final PresenceNotificationHandler presenceNotificationHandler;
  private final String host;
  private final int port;
  private final long accessKey;
  private final long accountId;
  private final String destination;
  private final String group;
  private final String accessToken;
  private final byte[] accessTokenBytes;
  private final RequestHandlerRegistry registry;
  private final RSocketBarrier barrier;
  private final boolean keepalive;
  private final long tickPeriodSeconds;
  private final long ackTimeoutSeconds;
  private final int missedAcks;
  private final int connectionPool = Runtime.getRuntime().availableProcessors();
  private volatile boolean running = true;

  private NetifiServer(
      String host,
      int port,
      long accessKey,
      long accountId,
      String destination,
      String group,
      String accessToken,
      byte[] accessTokenBytes,
      boolean keepalive,
      long tickPeriodSeconds,
      long ackTimeoutSeconds,
      int missedAcks) {
    this.registry = new DefaultRequestHandlerRegistry();
    this.host = host;
    this.port = port;
    this.accessKey = accessKey;
    this.accountId = accountId;
    this.destination = destination;
    this.group = group;
    this.accessToken = accessToken;
    this.accessTokenBytes = accessTokenBytes;
    this.idGenerator = new TimebasedIdGenerator(destination.hashCode());
    this.keepalive = keepalive;
    this.tickPeriodSeconds = tickPeriodSeconds;
    this.ackTimeoutSeconds = ackTimeoutSeconds;
    this.missedAcks = missedAcks;
    this.barrier =
        new LoadBalancedRSocketBarrier(
            host,
            port,
            Runtime.getRuntime().availableProcessors(),
            idGenerator,
            destination,
            group,
            accountId,
            registry);

    this.presenceNotificationHandler =
        new DefaultPresenceNotificationHandler(
            barrier, () -> running, idGenerator, accountId, destination);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Flowable<Collection<String>> presence(long accountId, String group) {
    return presenceNotificationHandler.presence(accountId, group);
  }

  @Override
  public Flowable<Collection<String>> presence(long accountId, String group, String destination) {
    return presenceNotificationHandler.presence(accountId, group, destination);
  }

  @Override
  public String toString() {
    return "Netifi{"
        + "idGenerator="
        + idGenerator
        + ", host='"
        + host
        + '\''
        + ", port="
        + port
        + ", accessKey="
        + accessKey
        + ", accountId="
        + accountId
        + ", destination='"
        + destination
        + '\''
        + ", group='"
        + group
        + '\''
        + ", accessToken='"
        + accessToken
        + '\''
        + ", accessTokenBytes="
        + Arrays.toString(accessTokenBytes)
        + ", registry="
        + registry
        + ", barrier="
        + barrier
        + ", keepalive="
        + keepalive
        + ", tickPeriodSeconds="
        + tickPeriodSeconds
        + ", ackTimeoutSeconds="
        + ackTimeoutSeconds
        + ", missedAcks="
        + missedAcks
        + ", running="
        + running
        + '}';
  }

  public <T> T create(Class<T> service) {
    return create(service, null);
  }

  public <T> T create(Class<T> service, String destination) {
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

    return create(service, accountId, group, destination);
  }

  public <T> T create(Class<T> service, long accountId, String group) {
    return create(service, accountId, group, null);
  }

  public <T> T create(Class<T> service, long accountId, String group, String destination) {
    Objects.requireNonNull(service, "service must be non-null");

    logger.info(
        "creating service {}, accountId {}, group {}, destination {}",
        service,
        accountId,
        group,
        destination);

    Object o =
        Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[] {service},
            new NetifiInvocationHandler(
                barrier,
                accountId,
                group,
                destination,
                this.accountId,
                this.group,
                this.destination,
                this.idGenerator));

    return (T) o;
  }

  public <T1, T2> void registerHandler(Class<T1> clazz, T2 t) {
    logger.info(
        "registering class {}, with interface {}", t.getClass().toString(), clazz.toString());
    registry.registerHandler(clazz, t);
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

    public Builder accountId(long accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder destination(String destination) {
      this.destination = destination;

      return this;
    }

    public NetifiServer build() {
      Objects.requireNonNull(host, "host is required");
      Objects.requireNonNull(port, "port is required");
      Objects.requireNonNull(accountId, "account Id is required");
      Objects.requireNonNull(group, "group is required");
      Objects.requireNonNull(destination, "destination id is required");

      logger.info(
          "registering with netifi with account id {}, group {}, and destination {}",
          accountId,
          group,
          destination);

      return new NetifiServer(
          host,
          port,
          accessKey == null ? 0 : accessKey,
          accountId,
          destination,
          group,
          accessToken,
          accessTokenBytes,
          keepalive,
          tickPeriodSeconds,
          ackTimeoutSeconds,
          missedAcks);
    }
  }
}
