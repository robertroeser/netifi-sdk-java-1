package io.netifi.sdk.rs;

import io.netifi.sdk.frames.RouteDestinationFlyweight;
import io.netifi.sdk.frames.RoutingFlyweight;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.util.PayloadImpl;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;

public class DefaultNetifiSocketTest {
  private static final TimebasedIdGenerator idGenerator = new TimebasedIdGenerator(1);

  @Test
  public void testRequestResponse() {
    byte[] token = new byte[20];
    ThreadLocalRandom.current().nextBytes(token);
    MonoProcessor<Void> onClose = MonoProcessor.create();
    ReconnectingRSocket mock = Mockito.mock(ReconnectingRSocket.class);
    Mockito.when(mock.onClose()).thenReturn(onClose);
    Mockito.when(mock.getCurrentSessionCounter()).thenReturn(Mono.just(new AtomicLong()));
    Mockito.when(mock.getCurrentSessionToken()).thenReturn(Mono.just(token));

    Mockito.when(mock.requestResponse(Mockito.any(Payload.class)))
        .then(
            invocation -> {
              Payload payload = (Payload) invocation.getArguments()[0];

              ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
              ByteBuf route = RoutingFlyweight.route(metadata);
              long accountId = RouteDestinationFlyweight.accountId(route);
              Assert.assertEquals(Long.MAX_VALUE, accountId);
              return Mono.just(new PayloadImpl("here's the payload"));
            });

    DefaultNetifiSocket netifiSocket =
        new DefaultNetifiSocket(
            mock,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            "fromDest",
            "toDest",
            "toGroup",
            token,
            false,
            idGenerator);

    byte[] metadata = new byte[1024];
    ThreadLocalRandom.current().nextBytes(metadata);

    netifiSocket
        .requestResponse(new PayloadImpl("hi".getBytes(), metadata))
        .doOnError(Throwable::printStackTrace)
        .block();
  }

  @Test
  public void testFireForget() {
    byte[] token = new byte[20];
    ThreadLocalRandom.current().nextBytes(token);
    MonoProcessor<Void> onClose = MonoProcessor.create();
    ReconnectingRSocket mock = Mockito.mock(ReconnectingRSocket.class);
    Mockito.when(mock.onClose()).thenReturn(onClose);
    Mockito.when(mock.getCurrentSessionCounter()).thenReturn(Mono.just(new AtomicLong()));
    Mockito.when(mock.getCurrentSessionToken()).thenReturn(Mono.just(token));

    Mockito.when(mock.fireAndForget(Mockito.any(Payload.class)))
        .then(
            invocation -> {
              Payload payload = (Payload) invocation.getArguments()[0];

              ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
              ByteBuf route = RoutingFlyweight.route(metadata);
              long accountId = RouteDestinationFlyweight.accountId(route);
              Assert.assertEquals(Long.MAX_VALUE, accountId);
              return Mono.empty();
            });

    DefaultNetifiSocket netifiSocket =
        new DefaultNetifiSocket(
            mock,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            "fromDest",
            "toDest",
            "toGroup",
            token,
            false,
            idGenerator);

    byte[] metadata = new byte[1024];
    ThreadLocalRandom.current().nextBytes(metadata);

    netifiSocket
        .fireAndForget(new PayloadImpl("hi".getBytes(), metadata))
        .doOnError(Throwable::printStackTrace)
        .block();
  }

  @Test
  public void testRequestStream() {
    byte[] token = new byte[20];
    ThreadLocalRandom.current().nextBytes(token);
    MonoProcessor<Void> onClose = MonoProcessor.create();
    ReconnectingRSocket mock = Mockito.mock(ReconnectingRSocket.class);
    Mockito.when(mock.onClose()).thenReturn(onClose);
    Mockito.when(mock.getCurrentSessionCounter()).thenReturn(Mono.just(new AtomicLong()));
    Mockito.when(mock.getCurrentSessionToken()).thenReturn(Mono.just(token));

    Mockito.when(mock.requestStream(Mockito.any(Payload.class)))
        .then(
            invocation -> {
              Payload payload = (Payload) invocation.getArguments()[0];

              ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
              ByteBuf route = RoutingFlyweight.route(metadata);
              long accountId = RouteDestinationFlyweight.accountId(route);
              Assert.assertEquals(Long.MAX_VALUE, accountId);
              return Flux.range(1, 100).map(i -> new PayloadImpl("here's the payload " + i));
            });

    DefaultNetifiSocket netifiSocket =
        new DefaultNetifiSocket(
            mock,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            "fromDest",
            "toDest",
            "toGroup",
            token,
            false,
            idGenerator);

    byte[] metadata = new byte[1024];
    ThreadLocalRandom.current().nextBytes(metadata);

    netifiSocket
        .requestStream(new PayloadImpl("hi".getBytes(), metadata))
        .doOnError(Throwable::printStackTrace)
        .blockLast();
  }

  @Test
  public void testRequestChannel() {
    byte[] token = new byte[20];
    ThreadLocalRandom.current().nextBytes(token);
    MonoProcessor<Void> onClose = MonoProcessor.create();
    ReconnectingRSocket mock = Mockito.mock(ReconnectingRSocket.class);
    Mockito.when(mock.onClose()).thenReturn(onClose);
    Mockito.when(mock.getCurrentSessionCounter()).thenReturn(Mono.just(new AtomicLong()));
    Mockito.when(mock.getCurrentSessionToken()).thenReturn(Mono.just(token));

    Mockito.when(mock.requestChannel(Mockito.any(Publisher.class)))
        .then(
            invocation -> {
              Publisher<Payload> payloads = (Publisher) invocation.getArguments()[0];

              return Flux.from(payloads)
                  .doOnNext(
                      payload -> {
                        ByteBuf metadata = Unpooled.wrappedBuffer(payload.getMetadata());
                        ByteBuf route = RoutingFlyweight.route(metadata);
                        long accountId = RouteDestinationFlyweight.accountId(route);
                        Assert.assertEquals(Long.MAX_VALUE, accountId);
                      })
                  .flatMap(
                      payload ->
                          Flux.range(1, 100).map(i -> new PayloadImpl("here's the payload " + i)));
            });

    DefaultNetifiSocket netifiSocket =
        new DefaultNetifiSocket(
            mock,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            "fromDest",
            "toDest",
            "toGroup",
            token,
            false,
            idGenerator);

    byte[] metadata = new byte[1024];
    ThreadLocalRandom.current().nextBytes(metadata);

    netifiSocket
        .requestChannel(Mono.just(new PayloadImpl("hi".getBytes(), metadata)))
        .doOnError(Throwable::printStackTrace)
        .blockLast();
  }
}
