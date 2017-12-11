package io.netifi.proteus.connection;

import io.rsocket.Closeable;
import io.rsocket.RSocket;
import reactor.core.publisher.Mono;

public interface NetifiRSocketFactory extends Closeable {

  Mono<RSocket> getRSocket();
}
