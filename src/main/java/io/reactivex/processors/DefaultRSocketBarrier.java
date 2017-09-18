package io.reactivex.processors;

import io.netifi.sdk.rs.RSocketBarrier;
import io.reactivex.Flowable;
import io.rsocket.RSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class DefaultRSocketBarrier implements RSocketBarrier {
  private static final Logger logger = LoggerFactory.getLogger(DefaultRSocketBarrier.class);
  private BehaviorProcessor<RSocket> behaviorProcessor;

  public DefaultRSocketBarrier() {
    behaviorProcessor = BehaviorProcessor.create();
  }

  @Override
  public Flowable<RSocket> getRSocket() {
    return behaviorProcessor.take(1);
  }

  public void setRSocket(RSocket rSocket) {
    rSocket
        .onClose()
        .doOnSubscribe(s -> behaviorProcessor.onNext(rSocket))
        .doFinally(
            s -> {
              logger.debug("setting RSocketBarrier state to invalid");
              behaviorProcessor.setCurrent(null);
            })
        .subscribe();
  }
}
