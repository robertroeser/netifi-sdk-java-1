package io.reactivex.processors;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.rsocket.AbstractRSocket;
import io.rsocket.RSocket;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/** Created by robertroeser on 9/13/17. */
public class RSocketBarrierTest {
  @Test
  public void testEmitValidRSocket() {
    RSocketBarrier barrier = new RSocketBarrier();
    barrier.setRSocket(new AbstractRSocket() {});
    RSocket rSocket = barrier.getRSocket().singleOrError().blockingGet();
    Assert.assertNotNull(rSocket);
  }

  @Test
  public void testEmitWhenSubscriberBeforeValidRSocketPresent() throws Exception {
    RSocketBarrier barrier = new RSocketBarrier();
    CountDownLatch latch = new CountDownLatch(1);
    barrier.getRSocket().singleOrError().doOnSuccess(r -> latch.countDown()).subscribe();
    barrier.setRSocket(new AbstractRSocket() {});
    latch.await();
  }

  @Test
  public void testMultipleThreadsUsingBarrierToGetRSocket() throws Exception {
    int count = 5000;
    CountDownLatch latch = new CountDownLatch(count);
    RSocketBarrier barrier = new RSocketBarrier();
    List<Flowable<RSocket>> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Flowable<RSocket> rSocketFlowable =
          barrier.getRSocket().doOnNext(rSocket -> latch.countDown()).subscribeOn(Schedulers.io());
      list.add(rSocketFlowable);
    }

    Flowable.merge(list).ignoreElements().subscribe();

    barrier.setRSocket(new AbstractRSocket() {});

    latch.await();
  }

  @Test
  public void testMultipleThreadsUsingBarrierWhenBothStatesHadGetCalled() throws Exception {
    int count = 5000;
    CountDownLatch latch = new CountDownLatch(count);
    RSocketBarrier barrier = new RSocketBarrier();
    List<Flowable<RSocket>> list = new ArrayList<>();
    boolean added = false;
    for (int i = 0; i < count; i++) {
      Flowable<RSocket> rSocketFlowable =
          barrier.getRSocket().doOnNext(rSocket -> latch.countDown()).subscribeOn(Schedulers.io());
      if (i > 2500) {
        if (!added) {
          added = true;
          barrier.setRSocket(new AbstractRSocket() {});
        }

        rSocketFlowable.subscribe();
      } else {
        list.add(rSocketFlowable);
      }
    }

    Flowable.merge(list).ignoreElements().subscribe();

    latch.await();
  }

}
