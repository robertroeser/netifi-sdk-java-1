package io.netifi.sdk;

import io.netifi.sdk.connection.Handler;
import io.netifi.sdk.serialization.RawPayload;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/** */
public class NetifiConfig {
  private static final String prefix = "io.netifi.sdk.";

  private String host = System.getProperty(prefix + ".host", "router.test.netifi.io");
  private int port = Integer.getInteger(prefix + ".port", 8800);
  private int major = Integer.getInteger(prefix + "major", 0);
  private int minor = Integer.getInteger(prefix + "minor", 0);
  private int patch = Integer.getInteger(prefix + "patch", 0);
  private long id = Long.getLong(prefix + "id", 0);
  private long environment = Long.getLong(prefix + "environment", 0);
  private long region = Long.getLong(prefix + "region", 0);
  private long group = Long.getLong(prefix + "group", 0);
  private long subGroup = Long.getLong(prefix + "subGroup", 0);
  private Function<Handler, Handler> handlerAcceptor =
      new Function<Handler, Handler>() {
        @Override
        public Handler apply(Handler handler) {
          return new Handler() {
            @Override
            public Mono<Void> fireAndForget(RawPayload payload) {
              return Mono.error(
                  new UnsupportedOperationException("Fire and forget not implemented."));
            }

            @Override
            public Mono<RawPayload> requestResponse(RawPayload payload) {
              return Mono.error(
                  new UnsupportedOperationException("Request-Response not implemented."));
            }

            @Override
            public Flux<RawPayload> requestStream(RawPayload payload) {
              return Flux.error(
                  new UnsupportedOperationException("Request-Stream not implemented."));
            }

            @Override
            public Flux<RawPayload> requestChannel(Publisher<RawPayload> payloads) {
              return Flux.error(
                  new UnsupportedOperationException("Request-Channel not implemented."));
            }

            @Override
            public Mono<Void> metadataPush(RawPayload payload) {
              return Mono.error(
                  new UnsupportedOperationException("Metadata-Push not implemented."));
            }
          };
        }
      };
  
  

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getMajor() {
    return major;
  }

  public void setMajor(int major) {
    this.major = major;
  }

  public int getMinor() {
    return minor;
  }

  public void setMinor(int minor) {
    this.minor = minor;
  }

  public int getPatch() {
    return patch;
  }

  public void setPatch(int patch) {
    this.patch = patch;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public long getEnvironment() {
    return environment;
  }

  public void setEnvironment(long environment) {
    this.environment = environment;
  }

  public long getRegion() {
    return region;
  }

  public void setRegion(long region) {
    this.region = region;
  }

  public long getGroup() {
    return group;
  }

  public void setGroup(long group) {
    this.group = group;
  }

  public long getSubGroup() {
    return subGroup;
  }

  public void setSubGroup(long subGroup) {
    this.subGroup = subGroup;
  }

  public Function<Handler, Handler> getHandlerAcceptor() {
    return handlerAcceptor;
  }

  public void setHandlerAcceptor(Function<Handler, Handler> handlerAcceptor) {
    this.handlerAcceptor = handlerAcceptor;
  }

  @Override
  public String toString() {
    return "NetifiConfig{"
        + "host='"
        + host
        + '\''
        + ", port="
        + port
        + ", major="
        + major
        + ", minor="
        + minor
        + ", patch="
        + patch
        + ", id="
        + id
        + ", environment="
        + environment
        + ", region="
        + region
        + ", group="
        + group
        + ", subGroup="
        + subGroup
        + '}';
  }
}
