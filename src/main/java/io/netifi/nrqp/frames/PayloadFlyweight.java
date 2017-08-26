package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;

/**
 * Flyweight used to get the data and metadata payloads
 */
public class PayloadFlyweight {
    public static ByteBuf data(ByteBuf metadata, ByteBuf data) {
        return null;
    }
    
    public static ByteBuf metadata(ByteBuf metadata, ByteBuf data) {
        return null;
    }
}
