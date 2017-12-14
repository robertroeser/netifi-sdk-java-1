package io.netifi.proteus.admin.connection;

import io.netifi.proteus.admin.frames.AdminFrameHeaderFlyweight;
import io.netifi.proteus.admin.frames.AdminFrameType;
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
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;

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

  @Test
  public void testStreamingFromNConnections() {
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
    
      Iterable<AdminRSocket> adminRSockets = manager.getRSockets().take(3).toIterable();

    Flux.fromIterable(adminRSockets)
        .flatMap(
            adminRSocket -> {
              System.out.println("streaming from ");
              return adminRSocket.requestStream(createPayload());
            })
        .take(10)
        .doOnNext(p -> System.out.println(p.toString()))
        .blockLast();
  }
    
    private Payload createPayload() {
        int length = AdminFrameHeaderFlyweight.computeLength("test");
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer(length);
        AdminFrameHeaderFlyweight.encode(
            byteBuf,
            AdminFrameType.ADMIN_FRAME_TRACE,
            System.currentTimeMillis(),
            Long.MAX_VALUE,
            "test");
        
        return ByteBufPayload.create(Unpooled.EMPTY_BUFFER, byteBuf);
    }
}
