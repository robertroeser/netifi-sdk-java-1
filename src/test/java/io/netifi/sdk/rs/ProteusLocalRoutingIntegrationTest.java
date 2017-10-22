package io.netifi.sdk.rs;

import io.netifi.sdk.Netifi;
import io.netifi.testing.protobuf.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Ignore
public class ProteusLocalRoutingIntegrationTest {

  private static final long accessKey = 3855261330795754807L;
  private static final String accessToken = "n9R9042eE1KaLtE56rbWjBIGymo=";

  private static Netifi server;
  private static Netifi client;
  private static NetifiSocket netifiSocket;

  @BeforeClass
  public static void setup() {
    server =
        Netifi.builder()
            .group("test.server")
            .destination("server")
            .accountId(Long.MAX_VALUE)
            .accessKey(accessKey)
            .accessToken(accessToken)
            .addHandler(new SimpleServiceServer(new DefaultSimpleService()))
            .host("127.0.0.1")
            .port(8001)
            .build();

    client =
        Netifi.builder()
            .group("test.client")
            .destination("client")
            .accountId(Long.MAX_VALUE)
            .accessKey(accessKey)
            .accessToken(accessToken)
            .host("127.0.0.1")
            .port(8001)
            .build();

    netifiSocket = client.connect("test.server").block();
  }

  @Test
  public void testUnaryRpc() {
    SimpleServiceClient simpleServiceClient = new SimpleServiceClient(netifiSocket);
    SimpleResponse simpleResponse =
        simpleServiceClient
            .unaryRpc(SimpleRequest.newBuilder().setRequestMessage("a message").build())
            .block();

    System.out.println(simpleResponse.getResponseMessage());
  }

  @Test
  public void testServerStreamingRpc() {
    SimpleServiceClient simpleServiceClient = new SimpleServiceClient(netifiSocket);
    SimpleResponse response = simpleServiceClient
                                  .serverStreamingRpc(SimpleRequest.newBuilder().setRequestMessage("a message").build())
                                  .take(10)
                                  .blockLast();

    System.out.println(response.getResponseMessage());
  }

  static class DefaultSimpleService implements SimpleService {
    @Override
    public Mono<SimpleResponse> unaryRpc(SimpleRequest message) {
      return Mono.fromCallable(
          () ->
              SimpleResponse.newBuilder()
                  .setResponseMessage("we got the message -> " + message.getRequestMessage())
                  .build());
    }

    @Override
    public Mono<SimpleResponse> clientStreamingRpc(Publisher<SimpleRequest> messages) {
      return Flux.from(messages)
          .take(10)
          .doOnNext(s -> System.out.println("got -> " + s.getRequestMessage()))
          .last()
          .map(
              simpleRequest ->
                  SimpleResponse.newBuilder()
                      .setResponseMessage("last one -> " + simpleRequest.getRequestMessage())
                      .build());

      /*
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
       */
    }

    @Override
    public Flux<SimpleResponse> serverStreamingRpc(SimpleRequest message) {
      String requestMessage = message.getRequestMessage();
      return Flux.interval(Duration.ofMillis(1))
          .onBackpressureDrop()
          .map(i -> i + " - got message - " + requestMessage)
          .map(s -> SimpleResponse.newBuilder().setResponseMessage(s).build());
    }

    @Override
    public Flux<SimpleResponse> bidiStreamingRpc(Publisher<SimpleRequest> messages) {
      return Flux.from(messages).flatMap(this::unaryRpc);
    }

    @Override
    public double availability() {
      return 1.0;
    }

    @Override
    public Mono<Void> close() {
      return Mono.empty();
    }

    @Override
    public Mono<Void> onClose() {
      return Mono.empty();
    }
  }
}
