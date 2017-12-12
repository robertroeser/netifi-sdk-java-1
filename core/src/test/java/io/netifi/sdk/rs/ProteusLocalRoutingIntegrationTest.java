package io.netifi.sdk.rs;

import io.netifi.sdk.Netifi;
import io.netifi.testing.protobuf.*;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import io.netty.buffer.ByteBuf;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Ignore
public class ProteusLocalRoutingIntegrationTest {

  private static final long accessKey = 3855261330795754807L;
  private static final String accessToken = "n9R9042eE1KaLtE56rbWjBIGymo=";

  private static Netifi server;
  private static Netifi client;
  private static NetifiSocket netifiSocket;

  private static final String host = "localhost";
  // private static final String host = "192.168.99.100";
  private static final int port = 8001;
  // private static final int server_port = 8001;
  private static final int server_port = 8001;

  @BeforeClass
  public static void setup() {
    server =
        Netifi.builder()
            .keepalive(false)
            .group("test.server")
            .destination("server")
            .accountId(Long.MAX_VALUE)
            .accessKey(accessKey)
            .accessToken(accessToken)
            .host(host)
            .port(server_port)
            .build();

    client =
        Netifi.builder()
            .keepalive(false)
            .group("test.client")
            .destination("client")
            .accountId(Long.MAX_VALUE)
            .accessKey(accessKey)
            .accessToken(accessToken)
            .host(host)
            .port(port)
            .build();

    server.addService(new SimpleServiceServer(new DefaultSimpleService()));

    netifiSocket = client.connect("test.server").block();
  }

  @Test
  public void testUnaryRpc() {
    SimpleServiceClient simpleServiceClient = new SimpleServiceClient(netifiSocket);
    SimpleResponse simpleResponse =
        simpleServiceClient
            .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("a message").build())
            .doOnError(Throwable::printStackTrace)
            .block();

    System.out.println(simpleResponse.getResponseMessage());
  }

  // @Test
  public void testUnaryRpc_multiple() {
    doTest();
    doTest();
  }

  public void doTest() {
    SimpleServiceClient simpleServiceClient = new SimpleServiceClient(netifiSocket);
    long start = System.nanoTime();
    Flux.range(1, 1_000_000)
        .flatMap(
            i ->
                simpleServiceClient
                    .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("a message").build())
                    .doOnError(Throwable::printStackTrace))
        .blockLast();

    double time = (System.nanoTime() - start) / 1_000_000d;
    double rps = 1_000_000 / (time / 1_000);
    System.out.println("time -> " + time + "ms");
    System.out.println("rps -> " + rps);

    // System.out.println(simpleResponse.getResponseMessage());
  }

  @Test
  public void testServerStreamingRpc() {
    SimpleServiceClient simpleServiceClient = new SimpleServiceClient(netifiSocket);
    SimpleResponse response =
        simpleServiceClient
            .serverStreamingRpc(SimpleRequest.newBuilder().setRequestMessage("a message").build())
            .take(100)
            .blockLast();

    System.out.println(response.getResponseMessage());
  }

  @Test
  public void testClientStreamingRpc() {
    SimpleServiceClient simpleServiceClient = new SimpleServiceClient(netifiSocket);
    Flux<SimpleRequest> map =
        Flux.range(1, 11)
            .map(i -> SimpleRequest.newBuilder().setRequestMessage("a message " + i).build());

    SimpleResponse response = simpleServiceClient.clientStreamingRpc(map).block();

    System.out.println(response.getResponseMessage());
  }

  @Test
  public void testBidiRequest() {
    SimpleServiceClient simpleServiceClient = new SimpleServiceClient(netifiSocket);

    Flux<SimpleRequest> map =
        Flux.range(1, 300_000)
            .map(i -> SimpleRequest.newBuilder().setRequestMessage("a message -> " + i).build());

    long count =
        simpleServiceClient
            .bidiStreamingRpc(map)
            //            .doOnNext(
            //                simpleResponse ->
            //                    System.out.println(
            //                        Thread.currentThread().getName()
            //                            + " - "
            //                            + simpleResponse.getResponseMessage()))
            .count()
            .block();

    System.out.println(count);
  }

  @Test
  public void testFireAndForget() throws Exception {
    int count = 100;
    CountDownLatch latch = new CountDownLatch(count);
    SimpleServiceClient client = new SimpleServiceClient(netifiSocket);
    client
        .streamOnFireAndForget(Empty.getDefaultInstance())
        .doOnError(Throwable::printStackTrace)
        .subscribe(simpleResponse -> latch.countDown());
    Flux.range(1, count)
        .log()
        .flatMap(
            i -> {
              System.out.println("fire -> " + i);
              return client.fireAndForget(
                  SimpleRequest.newBuilder().setRequestMessage("fire -> " + i).build());
            })
        .doOnError(Throwable::printStackTrace)
        .subscribe();
    latch.await();
  }

  static class DefaultSimpleService implements SimpleService {
    EmitterProcessor<SimpleRequest> messages = EmitterProcessor.create();

    @Override
    public Mono<Void> fireAndForget(SimpleRequest message, ByteBuf metadata) {
      messages.onNext(message);
      return Mono.empty();
    }

    @Override
    public Flux<SimpleResponse> streamOnFireAndForget(Empty message, ByteBuf metadata) {
      return messages.map(
          simpleRequest ->
              SimpleResponse.newBuilder()
                  .setResponseMessage("got fire and forget -> " + simpleRequest.getRequestMessage())
                  .build());
    }

    @Override
    public Mono<SimpleResponse> unaryRpc(SimpleRequest message, ByteBuf metadata) {
      return Mono.fromCallable(
          () ->
              SimpleResponse.newBuilder()
                  .setResponseMessage("we got the message -> " + message.getRequestMessage())
                  .build());
    }

    @Override
    public Mono<SimpleResponse> clientStreamingRpc(Publisher<SimpleRequest> messages, ByteBuf metadata) {
      return Flux.from(messages)
          .windowTimeout(10, Duration.ofSeconds(500))
          .take(1)
          .flatMap(Function.identity())
          .reduce(
              new ConcurrentHashMap<Character, AtomicInteger>(),
              (map, s) -> {
                char[] chars = s.getRequestMessage().toCharArray();
                for (char c : chars) {
                  map.computeIfAbsent(c, _c -> new AtomicInteger()).incrementAndGet();
                }

                return map;
              })
          .map(
              map -> {
                StringBuilder builder = new StringBuilder();

                map.forEach(
                    (character, atomicInteger) -> {
                      builder
                          .append("character -> ")
                          .append(character)
                          .append(", count -> ")
                          .append(atomicInteger.get())
                          .append("\n");
                    });

                String s = builder.toString();

                return SimpleResponse.newBuilder().setResponseMessage(s).build();
              });
    }

    @Override
    public Flux<SimpleResponse> serverStreamingRpc(SimpleRequest message, ByteBuf metadata) {
      String requestMessage = message.getRequestMessage();
      return Flux.interval(Duration.ofMillis(1))
          .publish()
          .refCount()
          .onBackpressureDrop()
          .map(i -> i + " - got message - " + requestMessage)
          .map(s -> SimpleResponse.newBuilder().setResponseMessage(s).build());
    }

    @Override
    public Flux<SimpleResponse> bidiStreamingRpc(Publisher<SimpleRequest> messages, ByteBuf metadata) {
      return Flux.from(messages).flatMap(message -> unaryRpc(message, metadata));
    }
  }
}
