package io.netifi.sdk;

import io.netifi.nrqp.frames.DestinationSetupFlyweight;
import io.netifi.nrqp.frames.RouteDestinationFlyweight;
import io.netifi.nrqp.frames.RoutingFlyweight;
import io.netifi.sdk.annotations.RequestResponse;
import io.netifi.sdk.annotations.RequestStream;
import io.netifi.sdk.annotations.Service;
import io.netifi.sdk.serializer.Serializers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.Flowable;
import io.rsocket.*;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/** */
@Ignore
public class IntegrationTest {
  @Test
  public void testReconnect() throws Exception {
    io.netifi.sdk.Netifi server =
        io.netifi.sdk.Netifi.builder()
            .accountId(100)
            .destinationId(2)
            .host("localhost")
            .port(8801)
            .group("test.group")
            .build();

    Thread.sleep(5_000);

    CountDownLatch latch = new CountDownLatch(1);

    RSocketFactory.receive()
        .acceptor(
            new SocketAcceptor() {
              ConcurrentHashMap<Long, RSocket> concurrentHashMap = new ConcurrentHashMap<>();

              @Override
              public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
                ByteBuf byteBuf = Unpooled.wrappedBuffer(setup.getMetadata());
                long destinationId = DestinationSetupFlyweight.destinationId(byteBuf);
                System.out.println("destination id " + destinationId + " connecting");
                concurrentHashMap.put(destinationId, sendingSocket);
                latch.countDown();
                return Mono.just(
                    new AbstractRSocket() {
                      @Override
                      public Mono<Payload> requestResponse(Payload payload) {
                        ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
                        ByteBuf route = RoutingFlyweight.route(metadata);
                        long destinationId1 = RouteDestinationFlyweight.destinationId(route);
                        RSocket rSocket = concurrentHashMap.get(destinationId1);
                        return rSocket.requestResponse(payload);
                      }

                      @Override
                      public Flux<Payload> requestStream(Payload payload) {
                        ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
                        ByteBuf route = RoutingFlyweight.route(metadata);
                        long destinationId1 = RouteDestinationFlyweight.destinationId(route);
                        RSocket rSocket = concurrentHashMap.get(destinationId1);
                        return rSocket.requestStream(payload);
                      }
                    });
              }
            })
        .transport(TcpServerTransport.create("localhost", 8801))
        .start()
        .block();

    latch.await();
  }

  @Test
  public void testReconnectWithKeepAlive() throws Exception {
    io.netifi.sdk.Netifi server =
        io.netifi.sdk.Netifi.builder().accountId(100).destinationId(2).group("test.group").build();

    LockSupport.park();
  }

  @Test
  public void test() throws Exception {
    /*RSocketBarrier.receive()
    .acceptor(
        new SocketAcceptor() {
          ConcurrentHashMap<Long, RSocket> concurrentHashMap = new ConcurrentHashMap<>();

          @Override
          public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
            ByteBuf byteBuf = Unpooled.wrappedBuffer(setup.getMetadata());
            long destinationId = DestinationSetupFlyweight.destinationId(byteBuf);
            System.out.println("destination id " + destinationId + " connecting");
            concurrentHashMap.put(destinationId, sendingSocket);
            return Mono.just(
                new AbstractRSocket() {
                  @Override
                  public Mono<Payload> requestResponse(Payload payload) {
                    ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
                    ByteBuf route = RoutingFlyweight.route(metadata);
                    long destinationId1 = RouteDestinationFlyweight.destinationId(route);
                    RSocket rSocket = concurrentHashMap.get(destinationId1);
                    return rSocket.requestResponse(payload);
                  }

                  @Override
                  public Flux<Payload> requestStream(Payload payload) {
                    ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
                    ByteBuf route = RoutingFlyweight.route(metadata);
                    long destinationId1 = RouteDestinationFlyweight.destinationId(route);
                    RSocket rSocket = concurrentHashMap.get(destinationId1);
                    return rSocket.requestStream(payload);
                  }
                });
          }
        })
    .transport(TcpServerTransport.create("localhost", 8801))
    .start()
    .block();*/

    io.netifi.sdk.Netifi server =
        io.netifi.sdk.Netifi.builder()
            .accountId(100)
            .destinationId(2)
            //.host("localhost")
            //.port(8801)
            .group("test.server")
            .build();

    io.netifi.sdk.Netifi server2 =
        io.netifi.sdk.Netifi.builder()
            .accountId(100)
            .destinationId(3)
            //.host("localhost")
            //.port(8801)
            .group("test.server")
            .build();

    server.registerHandler(TestService.class, new DefaultTestService());
    server2.registerHandler(TestService.class, new DefaultTestService());

    io.netifi.sdk.Netifi client =
        io.netifi.sdk.Netifi.builder()
            .accountId(100)
            .destinationId(1)
            //.host("localhost")
            //.port(8801)
            .group("test.client")
            .build();

    io.netifi.sdk.Netifi client2 =
        io.netifi.sdk.Netifi.builder()
            .accountId(100)
            .destinationId(4)
            //.host("localhost")
            //.port(8801)
            .group("test.client")
            .build();

    TestService testService = client.create(TestService.class);
    TestService testService2 = client2.create(TestService.class);

    String s1 = testService.test(1234).doOnError(Throwable::printStackTrace).blockingLast();
    Assert.assertEquals("1234", s1);

    Flowable.merge(testService.getTicks(), testService2.getTicks())
        .flatMap(i -> testService.test(i))
        .take(5)
        .doOnNext(s -> System.out.println("got " + s))
        .blockingLast();

    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.putInt(3);
    buffer.flip();

    List<ByteBuffer> byteBuffers = testService.get(buffer).toList().blockingGet();
    Assert.assertEquals(3, byteBuffers.size());
  }

  @Service(accountId = 100, group = "test.server")
  public interface TestService {
    @RequestResponse
    Flowable<String> test(Integer integer);

    @RequestStream
    Flowable<Integer> getTicks();

    @RequestStream(serializer = Serializers.BINARY)
    Flowable<ByteBuffer> get(ByteBuffer buffer);
  }

  public static class DefaultTestService implements TestService {
    @Override
    public Flowable<String> test(Integer integer) {
      return Flowable.just(String.valueOf(integer));
    }

    @Override
    public Flowable<Integer> getTicks() {
      return Flowable.interval(250, TimeUnit.MILLISECONDS)
          .map(i -> (int) System.currentTimeMillis());
    }

    @Override
    public Flowable<ByteBuffer> get(ByteBuffer buffer) {
      int anInt = buffer.getInt();
      System.out.println("sending " + anInt);
      return Flowable.range(1, anInt)
          .map(
              i -> {
                byte[] bytes = new byte[1024];
                ThreadLocalRandom.current().nextBytes(bytes);
                return ByteBuffer.wrap(bytes);
              });
    }
  }
}
