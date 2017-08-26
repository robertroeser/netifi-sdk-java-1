package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;

/**
 *
 */
public class AuthenticationResponseFlyweight {
    private static final int RESPONSE_SIZE = BitUtil.SIZE_OF_BYTE;
    
    private AuthenticationResponseFlyweight() {}
    
    public static int computeLength() {
        return FrameHeaderFlyweight.computeFrameHeaderLength() + RESPONSE_SIZE;
    }
    
    public static int encode(ByteBuf byteBuf, boolean authenticated, long seqId) {
        int offset = FrameHeaderFlyweight.encodeFrameHeader(byteBuf, FrameType.AUTH_RESPONSE, 0, seqId);
        byteBuf.setBoolean(offset, authenticated);
        offset += RESPONSE_SIZE;
        return offset;
    }
    
    public static boolean authenticated(ByteBuf byteBuf) {
        int offset = FrameHeaderFlyweight.computeFrameHeaderLength();
        return byteBuf.getBoolean(offset);
    }
}
