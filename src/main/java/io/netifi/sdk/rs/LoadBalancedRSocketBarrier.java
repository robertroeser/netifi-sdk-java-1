package io.netifi.sdk.rs;

import io.netifi.nrqp.frames.DestinationSetupFlyweight;
import io.netifi.sdk.NetifiServer;
import io.netifi.sdk.RequestHandlerRegistry;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.Flowable;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.client.LoadBalancedRSocketMono;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.PayloadImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/** */
public class LoadBalancedRSocketBarrier implements RSocketBarrier {

  static final Throwable CONNECTION_CLOSED = new Throwable("connection is closed");
  private static final Logger logger = LoggerFactory.getLogger(NetifiServer.class);
  private final LoadBalancedRSocketMono balancedRSocketMono;
  private final TimebasedIdGenerator idGenerator;
  private final String destination;
  private final String group;
  private final long accountId;
  private final RequestHandlerRegistry registry;

  private final String host;
  private final int port;

  public LoadBalancedRSocketBarrier(
      String host,
      int port,
      int numConnections,
      TimebasedIdGenerator idGenerator,
      String destination,
      String group,
      long accountId,
      RequestHandlerRegistry registry) {
    List<RSocketSupplier> suppliers = new ArrayList<>();
    for (int i = 0; i < numConnections; i++) {
      final int _i = i;
      suppliers.add(new RSocketSupplier(() -> getRSocketMono(_i)));
    }

    this.host = host;
    this.port = port;
    this.idGenerator = idGenerator;
    this.destination = destination;
    this.group = group;
    this.accountId = accountId;
    this.registry = registry;

    this.balancedRSocketMono = LoadBalancedRSocketMono.create(Flux.just(suppliers));
  }

  private Mono<RSocket> getRSocketMono(int i) {
    String _destination = destination + "-" + i;
    int length = DestinationSetupFlyweight.computeLength(false, _destination, group);
    byte[] bytes = new byte[length];
    ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
    DestinationSetupFlyweight.encode(
        byteBuf,
        Unpooled.EMPTY_BUFFER,
        Unpooled.wrappedBuffer(new byte[20]),
        idGenerator.nextId(),
        accountId,
        _destination,
        group);

    RSocketFactory.ClientRSocketFactory connect = RSocketFactory.connect();

    return connect
        .errorConsumer(throwable -> logger.error("unhandled error", throwable))
        .setupPayload(new PayloadImpl(new byte[0], bytes))
        .acceptor(
            rSocket -> {
              logger.info(
                  "destination with id " + _destination + " connected to " + host + ":" + port);

              return new RequestHandlingRSocket(registry);
            })
        .transport(TcpClientTransport.create(host, port))
        .start();
  }

  @Override
  public Flowable<RSocket> getRSocket() {
    return Flowable.fromPublisher(balancedRSocketMono);
  }
}