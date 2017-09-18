package io.netifi.sdk.rs;

import io.reactivex.Flowable;
import io.rsocket.RSocket;

/**
 *
 */
public interface RSocketBarrier {
    Flowable<RSocket> getRSocket();
}
