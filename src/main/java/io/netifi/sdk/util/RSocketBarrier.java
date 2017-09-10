package io.netifi.sdk.util;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.rsocket.RSocket;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * This class returns a {@code Flowable} that emits an RSocket when there is a valid RSocket. If the
 * RSocket isn't valid the other Flowable will not emit until a valid RSocket is available again.
 * This will prevent callers from continuing until the barrier is supplied with a RSocket. When the
 * RSocket is closed it will set its state to invalid until an open RSocket is supplied again.
 */
public class RSocketBarrier {
  private static final AtomicIntegerFieldUpdater<RSocketBarrier> WIP =
      AtomicIntegerFieldUpdater.newUpdater(RSocketBarrier.class, "wip");
  // Visible for Testing
  volatile State state = State.INVALID;
  private volatile RSocket rSocket;

  @SuppressWarnings("unused")
  private volatile int wip;

  private ArrayDeque<FlowableEmitter<RSocket>> emitters;

  public RSocketBarrier() {
    emitters = new ArrayDeque<>();
  }

  public Flowable<RSocket> getRSocket() {
    return Flowable.create(
        e -> {
          if (state == State.VALID) {
            e.onNext(rSocket);
            e.onComplete();
          } else {
            WIP.set(this, 1);
            synchronized (RSocketBarrier.this) {
              emitters.add(e);
            }
          }
        },
        BackpressureStrategy.BUFFER);
  }

  public void setRSocket(RSocket rSocket) {
    this.rSocket = rSocket;
    this.state = State.VALID;
    rSocket.onClose().doFinally(s -> state = State.INVALID).subscribe();
    drain();
  }

  void drain() {
    if (WIP.get(this) > 0) {
      for (; ; ) {
        WIP.set(this, 0);

        for (; ; ) {
          boolean empty;
          State s;
          synchronized (this) {
            s = state;
            empty = emitters.isEmpty();
          }

          if (empty || s == State.INVALID) {
            break;
          }

          FlowableEmitter<RSocket> emitter;
          synchronized (this) {
            emitter = emitters.poll();
          }

          if (emitter != null) {
            emitter.onNext(rSocket);
            emitter.onComplete();
          }
        }

        if (WIP.get(this) == 0) {
          break;
        }
      }
    }
  }

  enum State {
    VALID,
    INVALID
  }
}
