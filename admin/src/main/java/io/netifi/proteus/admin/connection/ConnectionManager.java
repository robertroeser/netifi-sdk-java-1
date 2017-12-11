package io.netifi.proteus.admin.connection;

import io.rsocket.RSocket;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ConnectionManager {
    /**
     * Returns a single connection to a router
     * @return
     */
    Mono<RSocket> getRSocket();
    
    /**
     * Streams all active connections
     * @return
     */
    Flux<? extends RSocket> getRSockets();
}
