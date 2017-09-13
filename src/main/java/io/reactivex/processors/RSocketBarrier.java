package io.reactivex.processors;

import io.reactivex.Flowable;
import io.rsocket.RSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class RSocketBarrier {
  private static final Logger logger = LoggerFactory.getLogger(RSocketBarrier.class);
  private BehaviorProcessor<RSocket> behaviorProcessor;

  public RSocketBarrier() {
    behaviorProcessor = BehaviorProcessor.create();
  }

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
