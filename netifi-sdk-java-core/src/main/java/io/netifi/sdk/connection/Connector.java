package io.netifi.sdk.connection;

import io.netifi.sdk.NetifiConfig;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 *
 */
public interface Connector<R extends Connection> extends Function<NetifiConfig, Mono<R>> {
}
