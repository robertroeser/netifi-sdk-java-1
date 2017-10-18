package io.netifi.sdk.rs;

import io.netifi.proteus.frames.DestinationSetupFlyweight;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.local.LocalClientTransport;
import io.rsocket.transport.local.LocalServerTransport;
import io.rsocket.util.PayloadImpl;
import org.junit.Test;
import reactor.core.publisher.Mono;

public class ReconnectingRSocketTest {
  @Test(expected = Exception.class)
  public void testConnection() {
    RequestHandlingRSocket handlingRSocket =
        new RequestHandlingRSocket(
            new AbstractRSocket() {
              private int PACKAGE_ID = 1;
              private int SERVICE_ID = 2;

              @Override
              public Mono<Payload> requestResponse(Payload payload) {
                return Mono.just(new PayloadImpl("hi"));
              }
            });

    int length = DestinationSetupFlyweight.computeLength(false, "dest", "test.group");
    byte[] metadata = new byte[length];

    ByteBuf byteBuf = Unpooled.wrappedBuffer(metadata);
    DestinationSetupFlyweight.encode(
        byteBuf,
        Unpooled.EMPTY_BUFFER,
        Unpooled.wrappedBuffer(new byte[20]),
        System.currentTimeMillis(),
        1L,
        "dest",
        "test.group");
    byte[] empty = new byte[0];

    RSocketFactory.receive()
        .acceptor((setup, sendingSocket) -> Mono.just(handlingRSocket))
        .transport(LocalServerTransport.create("test"))
        .start()
        .block();

    ReconnectingRSocket rSocket =
        new ReconnectingRSocket(
            handlingRSocket,
            () -> new PayloadImpl(empty, metadata),
            () -> true,
            () -> LocalClientTransport.create("test"),
            false,
            1,
            new byte[20]);

    rSocket.requestResponse(new PayloadImpl("hi")).block();
  }
}
