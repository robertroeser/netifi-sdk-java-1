package io.netifi.sdk;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

/** */
public abstract class Netifi {
  public static Netifi newInstance() {
    return newInstance(new NetifiConfig());
  }

  public static Netifi newInstance(NetifiConfig netifiConfig) {
    return new DefaultNetifi(netifiConfig);
  }

  public abstract <T> T findServiceById(Class<T> clazz, String id);

  public abstract <T> T findService(Class<T> clazz, String group);

  public abstract <T> T findService(Class<T> clazz, String group, String subgroup);

  public abstract Mono<Boolean> isIdConnected(String id);

  public abstract Mono<Boolean> isConnected(String group);

  public abstract Mono<Boolean> isConnected(String group, String subgroup);

  public Mono<Boolean> notifyWhenIdConnected(String id) {
    return notifyWhenIdConnected(id, Duration.ofSeconds(30));
  }

  public Mono<Boolean> notifyWhenConnected(String group) {
    return notifyWhenConnected(group, Duration.ofSeconds(30));
  }

  public Mono<Boolean> notifyWhenConnected(String group, String subgroup) {
    return notifyWhenConnected(group, subgroup, Duration.ofSeconds(30));
  }

  public Mono<Boolean> notifyWhenIdConnected(String id, Duration timeout) {
    return notifyWhen(() -> isIdConnected(id), timeout);
  }

  public Mono<Boolean> notifyWhenConnected(String group, Duration timeout) {
    return notifyWhen(() -> isConnected(group), timeout);
  }

  public Mono<Boolean> notifyWhenConnected(String group, String subgroup, Duration timeout) {
    return notifyWhen(() -> isConnected(group, subgroup), timeout);
  }

  Mono<Boolean> notifyWhen(Supplier<Mono<Boolean>> condition, Duration timeout) {
    return Flux.interval(Duration.ofMillis(0), Duration.ofMillis(500))
        .flatMap(t -> condition.get())
        .takeUntil(b -> b)
        .timeout(timeout)
        .single();
  }

  public abstract <T> void register(T t);
}
