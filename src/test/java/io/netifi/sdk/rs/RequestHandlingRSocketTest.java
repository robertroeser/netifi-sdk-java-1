package io.netifi.sdk.rs;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import org.junit.Test;
import reactor.core.publisher.Mono;

import static org.junit.Assert.*;

public class RequestHandlingRSocketTest {
    @Test
    public void testRequestHandlder() {
        RequestHandlingRSocket rSocket = new RequestHandlingRSocket(new AbstractRSocket() {
            private int PACKAGE_ID = 1;
            private int SERVICE_ID = 2;
        });
    }
}