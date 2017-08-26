package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import static io.netifi.nrqp.frames.FrameHeaderFlyweight.ENCRYPTED;

/** */
public class DestinationSetupFlyweight {

  private static final int PUBLIC_KEY_SIZE = 32;
  private static final int ACCESS_TOKEN_SIZE = 20;
  private static final int ACCESS_KEY_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int DESTINATION_ID_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int GROUP_ID_SIZE = BitUtil.SIZE_OF_LONG;
  
  private DestinationSetupFlyweight() {}
  
  public static int computeLength(boolean encrypted, int numGroups) {

    return FrameHeaderFlyweight.computeFrameHeaderLength()
        + (encrypted ? PUBLIC_KEY_SIZE : 0)
        + ACCESS_TOKEN_SIZE
        + ACCESS_KEY_SIZE
        + DESTINATION_ID_SIZE
        + GROUP_ID_SIZE * numGroups;
  }

  public static int encode(
      ByteBuf byteBuf,
      ByteBuf publicKey,
      ByteBuf accessToken,
      long accessKey,
      long destinationId,
      long seqId,
      long... groupIds) {

    if (accessToken.capacity() != ACCESS_TOKEN_SIZE) {
      throw new IllegalStateException(
          String.format(
              "invalid access token size: found %d, expected %d",
              accessToken.capacity(), ACCESS_TOKEN_SIZE));
    }

    boolean encrypted = publicKey != null && publicKey.capacity() > 0;

    if (encrypted && publicKey.capacity() != PUBLIC_KEY_SIZE) {
      throw new IllegalStateException(
          String.format(
              "invalid public key size: found %d, expected %d",
              publicKey.capacity(), PUBLIC_KEY_SIZE));
    }

    int numGroups = groupIds == null ? 0 : groupIds.length;

    int size = computeLength(encrypted, numGroups);

    if (byteBuf.capacity() < size) {
      byteBuf.capacity(size);
    }

    int offset =
        FrameHeaderFlyweight.encodeFrameHeader(
            byteBuf, FrameType.DESTINATION_SETUP, encrypted ? ENCRYPTED : 0, seqId);

    if (encrypted) {
      byteBuf.setBytes(offset, publicKey);
      offset += PUBLIC_KEY_SIZE;
    }

    byteBuf.setBytes(offset, accessToken);
    offset += ACCESS_TOKEN_SIZE;

    byteBuf.setLong(offset, accessKey);
    offset += ACCESS_KEY_SIZE;

    byteBuf.setLong(offset, destinationId);
    offset += DESTINATION_ID_SIZE;

    int groupIdsLength = groupIds.length;
    for (int i = 0; i < groupIdsLength; i++) {
      byteBuf.setLong(offset, groupIds[i]);
      offset += GROUP_ID_SIZE;
    }

    return size;
  }

  public static ByteBuf publicKey(ByteBuf byteBuf) {
    boolean encrypted = FrameHeaderFlyweight.encrypted(byteBuf);
    if (encrypted) {
      int offset = FrameHeaderFlyweight.computeFrameHeaderLength();
      return byteBuf.slice(offset, PUBLIC_KEY_SIZE);
    } else {
      return Unpooled.EMPTY_BUFFER;
    }
  }

  public static ByteBuf accessToken(ByteBuf byteBuf) {
    int offset = calculatePublicKeyOffset(byteBuf);
    return byteBuf.slice(offset, ACCESS_TOKEN_SIZE);
  }

  public static long accessKey(ByteBuf byteBuf) {
    int offset = calculatePublicKeyOffset(byteBuf) + ACCESS_TOKEN_SIZE;
    return byteBuf.getLong(offset);
  }

  public static long destinationId(ByteBuf byteBuf) {
    int offset = calculatePublicKeyOffset(byteBuf) + ACCESS_TOKEN_SIZE + ACCESS_KEY_SIZE;
    return byteBuf.getLong(offset);
  }

  private static int calculatePublicKeyOffset(ByteBuf byteBuf) {
    boolean encrypted = FrameHeaderFlyweight.encrypted(byteBuf);
    return (encrypted ? PUBLIC_KEY_SIZE : 0) + FrameHeaderFlyweight.computeFrameHeaderLength();
  }
  
  public static long[] groupIds(ByteBuf byteBuf) {
    int offset =
        calculatePublicKeyOffset(byteBuf)
            + ACCESS_TOKEN_SIZE
            + ACCESS_KEY_SIZE
            + DESTINATION_ID_SIZE;
    int remaining = (byteBuf.capacity() - offset) / GROUP_ID_SIZE;
    long[] groupIds = remaining == 0 ? BitUtil.EMPTY : new long[remaining];
    for (int i = 0; i < remaining; i++) {
      long groupId = byteBuf.getLong(offset);
      groupIds[i] = groupId;
      offset += GROUP_ID_SIZE;
    }

    return groupIds;
  }
}
