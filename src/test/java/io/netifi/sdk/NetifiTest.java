package io.netifi.sdk;

import io.netifi.sdk.annotations.REQUEST_RESPONSE;
import io.netifi.sdk.serializer.Serializers;
import io.reactivex.Flowable;

import java.nio.ByteBuffer;

/** Created by robertroeser on 9/3/17. */
public class NetifiTest {

  public void test() {
    Netifi netifi = Netifi.builder().accessKey(123).host("router.netifi.io").port(7001).build();

    netifi.registerHandler(new SomeServiceImpl(), SomeServiceImpl.class);

    SomeService someService = netifi.create(SomeService.class, 123, "test.uswest");
    String s = someService.getUserName(123).blockingFirst();
    System.out.println("there name " + s);
  }

  interface SomeService {

    @REQUEST_RESPONSE(serializer = Serializers.CBOR)
    Flowable<String> getUserName(int userId);

    @REQUEST_RESPONSE(serializer = Serializers.BINARY)
    Flowable<ByteBuffer> getUserId(ByteBuffer buffer);
  }

  class SomeServiceImpl implements SomeService {
    @Override
    public Flowable<String> getUserName(int userId) {
      return Flowable.just("hi");
    }

    @Override
    public Flowable<ByteBuffer> getUserId(ByteBuffer buffer) {
      return Flowable.just(ByteBuffer.wrap("hi".getBytes()));
    }
  }
}
