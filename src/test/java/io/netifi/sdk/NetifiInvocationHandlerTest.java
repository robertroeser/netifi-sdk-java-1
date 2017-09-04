package io.netifi.sdk;

import io.netifi.sdk.annotations.REQUEST_RESPONSE;
import io.netifi.sdk.annotations.REQUEST_STREAM;
import io.netifi.sdk.serializer.JSONSerializer;
import io.netifi.sdk.serializer.Serializers;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.reactivex.Flowable;
import io.reactivex.processors.ReplayProcessor;
import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.util.PayloadImpl;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.List;

/** */
public class NetifiInvocationHandlerTest {
  @Test
  public void testRequestResponseInvocation() throws Exception {
    ReplayProcessor<RSocket> rSocketPublishProcessor = ReplayProcessor.create();
    rSocketPublishProcessor.onNext(new TestSocket());
    long accountId = 1;
    String group = "foo.bar.baz";
    long destination = -1;

    TestService testService =
        (TestService)
            Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {TestService.class},
                new NetifiInvocationHandler(
                    rSocketPublishProcessor,
                    accountId,
                    group,
                    destination,
                    1,
                    new long[] {2},
                    3,
                    new TimebasedIdGenerator(1)));

    String s = testService.test(1234).blockingFirst();
    Assert.assertEquals("hi", s);
  }

  @Test
  public void testRequestResponseInvocationNoArgs() throws Exception {
    ReplayProcessor<RSocket> rSocketPublishProcessor = ReplayProcessor.create();
    rSocketPublishProcessor.onNext(new TestSocket());
    long accountId = 1;
    String group = "foo.bar.baz";
    long destination = -1;

    TestService testService =
        (TestService)
            Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {TestService.class},
                new NetifiInvocationHandler(
                    rSocketPublishProcessor,
                    accountId,
                    group,
                    destination,
                    1,
                    new long[] {2},
                    3,
                    new TimebasedIdGenerator(1)));

    String s = testService.noArgs().blockingFirst();
    Assert.assertEquals("hi", s);
  }

  @Test
  public void testStream() throws Exception {
    ReplayProcessor<RSocket> rSocketPublishProcessor = ReplayProcessor.create();
    rSocketPublishProcessor.onNext(new TestSocket());
    long accountId = 1;
    String group = "foo.bar.baz";
    long destination = -1;

    TestService testService =
        (TestService)
            Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[] {TestService.class},
                new NetifiInvocationHandler(
                    rSocketPublishProcessor,
                    accountId,
                    group,
                    destination,
                    1,
                    new long[] {2},
                    3,
                    new TimebasedIdGenerator(1)));

    List<Integer> integers = testService.stream().take(10).toList().blockingGet();
    Assert.assertEquals(10, integers.size());
  }

  public interface TestService {
    @REQUEST_RESPONSE(serializer = Serializers.JSON)
    Flowable<String> test(Integer integer);

    @REQUEST_RESPONSE(serializer = Serializers.JSON)
    Flowable<String> noArgs();

    @REQUEST_STREAM(serializer = Serializers.JSON)
    Flowable<Integer> stream();
  }

  static class TestSocket extends AbstractRSocket {
    @Override
    public Mono<Payload> requestResponse(Payload payload) {
      JSONSerializer<String> jsonSerializer = new JSONSerializer<>(String.class);
      ByteBuffer hi = jsonSerializer.serialize("hi");
      return Mono.just(new PayloadImpl(hi));
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
      JSONSerializer<Integer> jsonSerializer = new JSONSerializer<>(Integer.class);

      return Flux.range(1, 10)
          .doFinally(s -> System.out.println("done"))
          .map(
              i -> {
                ByteBuffer serialize = jsonSerializer.serialize(i);
                return new PayloadImpl(serialize);
              });
    }
  }
}
