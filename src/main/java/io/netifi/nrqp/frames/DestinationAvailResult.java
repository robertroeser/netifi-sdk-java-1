package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;

/** */
public class DestinationAvailResult {
  private static int TOKEN_SIZE = BitUtil.SIZE_OF_INT;
  private static int FOUND_SIZE = BitUtil.SIZE_OF_BYTE;

  public static int computeLength(boolean hasToken) {
    return FrameHeaderFlyweight.computeFrameHeaderLength() + (hasToken ? TOKEN_SIZE : 0) + FOUND_SIZE;
  }

  public static int encode(ByteBuf byteBuf, boolean hasToken, int token, boolean found, long seqId) {
    int flags = FrameHeaderFlyweight.encodeFlags(false, false, false, false, hasToken);
    int offset =
        FrameHeaderFlyweight.encodeFrameHeader(byteBuf, FrameType.DESTINATION_AVAIL_RESULT, flags, seqId);
      
    byteBuf.setByte(offset, found ? 1 : 0);

    return offset;
  }

  public static int token(ByteBuf byteBuf) {
    if (!FrameHeaderFlyweight.token(byteBuf)) {
      throw new IllegalStateException("no token present");
    }

    int offset = FrameHeaderFlyweight.computeFrameHeaderLength();
    return byteBuf.getInt(offset);
  }

  public static boolean found(ByteBuf byteBuf) {
    int offset = FrameHeaderFlyweight.computeFrameHeaderLength();
    if (FrameHeaderFlyweight.token(byteBuf)) {
      offset += TOKEN_SIZE;
    }
    return byteBuf.getByte(offset) == 1;
  }
}
