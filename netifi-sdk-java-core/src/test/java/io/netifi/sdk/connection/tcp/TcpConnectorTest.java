package io.netifi.sdk.connection.tcp;

import io.netifi.sdk.NetifiConfig;
import io.netifi.sdk.connection.Connectors;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** */
public class TcpConnectorTest {
    @Test
    public void tcpConnectorTest() {
        TcpConnector connector = new TcpConnector();
        NetifiConfig config = new NetifiConfig();
    
        Mono<TcpConnection> connectionMono = connector.apply(config);
    
        StepVerifier
            .create(connectionMono)
            .expectNextCount(1)
            .verifyComplete();
    }
    
    @Test
    public void testCachedConnection() {
        Connectors.CachingConnector<TcpConnection> connector = new Connectors.CachingConnector<>(new TcpConnector());
        NetifiConfig config = new NetifiConfig();
        Mono<TcpConnection> connectionMono = connector.apply(config);
        StepVerifier
            .create(connectionMono)
            .expectNextCount(1)
            .verifyComplete();
    
        connectionMono = connector.apply(config);
        StepVerifier
            .create(connectionMono)
            .expectNextCount(1)
            .verifyComplete();
    
        connectionMono = connector.apply(config);
        StepVerifier
            .create(connectionMono)
            .expectNextCount(1)
            .verifyComplete();
    }
}
