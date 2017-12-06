package io.netifi.sdk.balancer;

import io.netifi.sdk.balancer.transport.WeighedClientTransportSupplier;
import io.netifi.sdk.rs.WeightedRSocket;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

public class WeighedClientTransportSupplierTest {
  @Test
  public void testShouldDecrementActiveCountOnComplete() {

    MonoProcessor<Void> onClose = MonoProcessor.create();
    DuplexConnection duplexConnection = Mockito.mock(DuplexConnection.class);
    Mockito.when(duplexConnection.onClose()).thenReturn(onClose);

    ClientTransport transport = Mockito.mock(ClientTransport.class);
    Mockito.when(transport.connect()).thenReturn(Mono.just(duplexConnection));
    Supplier<ClientTransport> clientTransportSupplier = () -> transport;

    WeighedClientTransportSupplier supplier =
        new WeighedClientTransportSupplier(
            clientTransportSupplier, InetSocketAddress.createUnresolved("localhost", 8081));

    DirectProcessor<WeightedRSocket> p = DirectProcessor.create();
    DuplexConnection block = supplier.apply(p).get().connect().block();

    int i = supplier.activeConnections();

    Assert.assertEquals(1, i);

    onClose.onComplete();
    block.onClose().block();

    i = supplier.activeConnections();

    Assert.assertEquals(0, i);
  }
  
}
