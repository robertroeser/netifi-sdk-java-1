package io.netifi.sdk;

import io.netifi.auth.SessionUtil;
import io.netifi.nrqp.frames.DestinationSetupFlyweight;
import io.netifi.sdk.annotations.Service;
import io.netifi.sdk.rs.RequestHandlingRSocket;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.Flowable;
import io.reactivex.processors.DefaultRSocketBarrier;
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
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is where the magic happens
 */
public class Netifi implements AutoCloseable, PresenceNotificationHandler {
    static final Throwable CONNECTION_CLOSED = new Throwable("connection is closed");
    private static final Logger logger = LoggerFactory.getLogger(Netifi.class);
    
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
    private final DefaultRSocketBarrier barrier;
    private final Disposable disposable;
    private final boolean keepalive;
    private final long tickPeriodSeconds;
    private final long ackTimeoutSeconds;
    private final int missedAcks;
    private final SessionUtil sessionUtil = SessionUtil.instance();
    private final AtomicReference<AtomicLong> currentSessionCounter = new AtomicReference<>();
    private final AtomicReference<byte[]> currentSessionToken = new AtomicReference<>();
    private volatile boolean running = true;
    
    private Netifi(
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
        this.barrier = new DefaultRSocketBarrier();
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
        this.presenceNotificationHandler =
            new DefaultPresenceNotificationHandler(
                                                      barrier, () -> running, idGenerator, accountId, destination);
        
        AtomicLong delay = new AtomicLong();
        this.disposable =
            Mono.create(
                sink -> {
                    int length = DestinationSetupFlyweight.computeLength(false, destination, group);
                    byte[] bytes = new byte[length];
                    
                    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
                    DestinationSetupFlyweight.encode(
                        byteBuf,
                        Unpooled.EMPTY_BUFFER,
                        Unpooled.wrappedBuffer(accessTokenBytes),
                        idGenerator.nextId(),
                        accessKey,
                        destination,
                        group);
                    
                    RSocketFactory.ClientRSocketFactory connect = RSocketFactory.connect();
                    
                    if (keepalive) {
                        connect =
                            connect.keepAlive(
                                Duration.ofSeconds(tickPeriodSeconds),
                                Duration.ofSeconds(ackTimeoutSeconds),
                                missedAcks);
                    }
                    
                    connect
                        .errorConsumer(throwable -> logger.error("unhandled error", throwable))
                        .setupPayload(new PayloadImpl(new byte[0], bytes))
                        .acceptor(
                            rSocket -> {
                                rSocket
                                    .onClose()
                                    .doFinally(
                                        s -> {
                                            if (!running) {
                                                sink.success();
                                            } else {
                                                sink.error(CONNECTION_CLOSED);
                                            }
                                        })
                                    .subscribe();
                                long count = sessionUtil.getThirtySecondsStepsFromEpoch();
                                currentSessionCounter.set(new AtomicLong(count));
                                ByteBuffer allocate = ByteBuffer.allocate(8);
                                allocate.putLong(accessKey);
                                allocate.flip();
                                byte[] sessionToken = sessionUtil.generateSessionToken(accessTokenBytes, allocate, count);
                                currentSessionToken.set(sessionToken);
                                barrier.setRSocket(rSocket);
                                logger.info(
                                    "destination with id "
                                        + destination
                                        + " connected to "
                                        + host
                                        + ":"
                                        + port);
                                
                                return new RequestHandlingRSocket(registry);
                            })
                        .transport(TcpClientTransport.create(host, port))
                        .start()
                        .timeout(Duration.ofSeconds(5))
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
                .retry(
                    throwable -> {
                        if (running) {
                            logger.debug("Netifi is running, retrying connection");
                        }
                        
                        return running;
                    })
                .subscribe();
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
                   + ", disposable="
                   + disposable
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
                new Class<?>[]{service},
                new NetifiInvocationHandler(
                                               barrier,
                                               accountId,
                                               group,
                                               destination,
                                               this.accountId,
                                               this.group,
                                               this.destination,
                                               this.idGenerator,
                                               sessionUtil,
                                               currentSessionCounter,
                                               currentSessionToken));
        
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
        
        private Builder() {
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
                                 accessToken,
                                 accessTokenBytes,
                                 keepalive,
                                 tickPeriodSeconds,
                                 ackTimeoutSeconds,
                                 missedAcks);
        }
    }
}
