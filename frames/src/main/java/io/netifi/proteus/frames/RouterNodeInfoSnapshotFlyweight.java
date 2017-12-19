package io.netifi.proteus.frames;

import io.netty.buffer.ByteBuf;

public class RouterNodeInfoSnapshotFlyweight {
  public static int computeLength() {
    return FrameHeaderFlyweight.computeFrameHeaderLength();
  }

  public static int encode(ByteBuf byteBuf, long seqId) {

    int flags = FrameHeaderFlyweight.encodeFlags(false, false, false, false, false);
    return FrameHeaderFlyweight.encodeFrameHeader(byteBuf, FrameType.ROUTER_INFO_SNAPSHOT, flags, seqId);
  }
}
