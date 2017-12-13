package io.netifi.proteus.admin.connection;

import io.netifi.proteus.admin.frames.AdminSetupFlyweight;
import io.netifi.proteus.admin.rs.AdminRSocket;
import io.netifi.proteus.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import java.net.InetSocketAddress;
import java.util.Arrays;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Mono;

@Ignore
public class ConnectionManagerTest {
  @Test
  public void testGetConnections() {
    TimebasedIdGenerator idGenerator = new TimebasedIdGenerator(1);

    ConnectionManager manager =
        new DefaultConnectionManager(
            idGenerator,
            (socketAddress) -> {
              System.out.println("new connection to -> " + socketAddress.toString());
              AdminRSocket adminRSocket =
                  new AdminRSocket(
                      socketAddress,
                      socketAddress1 -> {
                        int length = AdminSetupFlyweight.computeLength();
                        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(length);
                        Payload payload = ByteBufPayload.create(Unpooled.EMPTY_BUFFER, byteBuf);
                        return RSocketFactory.connect()
                            .setupPayload(payload)
                            .transport(
                                TcpClientTransport.create((InetSocketAddress) socketAddress1))
                            .start();
                      },
                      new TimebasedIdGenerator(-1));

              return Mono.just(adminRSocket);
            },
            Arrays.asList(InetSocketAddress.createUnresolved("localhost", 6001)));

    manager.getRSockets().take(5).blockLast();
  }
}
