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
                    rSocketPublishProcessor, accountId, group, destination, new TimebasedIdGenerator(1)));

    String s = testService.test(1234).blockingFirst();
    System.out.println(s);
    Assert.assertEquals("1234", s);
  }

  public interface TestService {
    @REQUEST_RESPONSE(serializer = Serializers.JSON)
    Flowable<String> test(Integer integer);

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
          .map(
              i -> {
                ByteBuffer serialize = jsonSerializer.serialize(i);
                return new PayloadImpl(serialize);
              });
    }
  }
}
