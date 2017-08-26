package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Objects;

/** */
public class RoutingFlyweight {
  private static final int NAMESPACE_ID_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int CLASS_ID_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int METHOD_ID_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int USER_METADATA_LENGTH_SIZE = BitUtil.SIZE_OF_INT;
  private static final int ACCESS_KEY_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int DESTINATION_ID_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int GROUP_ID_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int TOKEN_SIZE = BitUtil.SIZE_OF_INT;

  private RoutingFlyweight() {}

  public static int computeLength(
      boolean apiCall, boolean metadata, boolean token, int numGroups, ByteBuf... routes) {
    int length = FrameHeaderFlyweight.computeFrameHeaderLength();
    if (routes != null) {
      for (ByteBuf byteBuf : routes) {
        length += byteBuf.capacity();
      }
    }

    if (apiCall) {
      length += NAMESPACE_ID_SIZE + CLASS_ID_SIZE + METHOD_ID_SIZE;
    }

    if (metadata) {
      length += USER_METADATA_LENGTH_SIZE;
    }

    if (token) {
      length += TOKEN_SIZE;
    }

    length += ACCESS_KEY_SIZE + DESTINATION_ID_SIZE; // + GROUP_ID_SIZE * numGroups;

    return length;
  }

  public static int encode(
      ByteBuf byteBuf,
      boolean apiCall,
      boolean hasToken,
      boolean hasMetadata,
      int token,
      long fromAccessKey,
      long fromDestinationId,
      int userMetadataLength,
      long namespaceId,
      long classId,
      long methodId,
      long seqId,
      ByteBuf... routes) {
    Objects.requireNonNull(routes, "routes must not be null");

    if (routes.length == 0) {
      throw new IllegalStateException("routes must not be empty");
    }

    int flags = FrameHeaderFlyweight.encodeFlags(true, hasMetadata, false, apiCall, hasToken);

    int offset = FrameHeaderFlyweight.encodeFrameHeader(byteBuf, FrameType.ROUTE, flags, seqId);

    if (hasToken) {
      byteBuf.setInt(offset, token);
      offset += TOKEN_SIZE;
    }

    byteBuf.setLong(offset, fromAccessKey);
    offset += ACCESS_KEY_SIZE;

    byteBuf.setLong(offset, fromDestinationId);
    offset += DESTINATION_ID_SIZE;

    if (hasMetadata) {
      byteBuf.setInt(offset, userMetadataLength);
      offset += USER_METADATA_LENGTH_SIZE;
    }

    if (apiCall) {
      byteBuf.setLong(offset, namespaceId);
      offset += NAMESPACE_ID_SIZE;

      byteBuf.setLong(offset, classId);
      offset += CLASS_ID_SIZE;

      byteBuf.setLong(offset, methodId);
      offset += METHOD_ID_SIZE;
    }

    for (ByteBuf route : routes) {
      byte[] bytes = new byte[route.capacity()];
      route.getBytes(0, bytes);
      byteBuf.setBytes(offset, bytes);
      offset += route.capacity();
    }

    return offset;
  }

  public static int token(ByteBuf byteBuf) {
    if (FrameHeaderFlyweight.token(byteBuf)) {
      int offset = FrameHeaderFlyweight.computeFrameHeaderLength();
      return byteBuf.getInt(offset);
    } else {
      throw new IllegalStateException("no token flag set");
    }
  }

  public static long accessKey(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0);

    return byteBuf.getLong(offset);
  }

  public static long destinationId(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE;

    return byteBuf.getLong(offset);
  }

  public static int userMetadataLength(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + DESTINATION_ID_SIZE;

    return byteBuf.getInt(offset);
  }

  public static long namespaceId(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + DESTINATION_ID_SIZE
            + +(FrameHeaderFlyweight.hasMetadata(byteBuf) ? USER_METADATA_LENGTH_SIZE : 0);

    return byteBuf.getLong(offset);
  }

  public static long classId(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + DESTINATION_ID_SIZE
            + (FrameHeaderFlyweight.hasMetadata(byteBuf) ? USER_METADATA_LENGTH_SIZE : 0)
            + NAMESPACE_ID_SIZE;

    return byteBuf.getLong(offset);
  }

  public static long methodId(ByteBuf byteBuf) {
    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + DESTINATION_ID_SIZE
            + (FrameHeaderFlyweight.hasMetadata(byteBuf) ? USER_METADATA_LENGTH_SIZE : 0)
            + NAMESPACE_ID_SIZE
            + CLASS_ID_SIZE;

    return byteBuf.getLong(offset);
  }

  public static List<ByteBuf> routes(ByteBuf byteBuf) {

    int offset =
        FrameHeaderFlyweight.computeFrameHeaderLength()
            + (FrameHeaderFlyweight.token(byteBuf) ? TOKEN_SIZE : 0)
            + ACCESS_KEY_SIZE
            + DESTINATION_ID_SIZE
            + (FrameHeaderFlyweight.hasMetadata(byteBuf) ? USER_METADATA_LENGTH_SIZE : 0);

    if (FrameHeaderFlyweight.apiCall(byteBuf)) {
      offset += NAMESPACE_ID_SIZE + CLASS_ID_SIZE + METHOD_ID_SIZE;
    }

    ByteBuf routes = byteBuf.slice(offset, byteBuf.capacity() - offset);

    return RouteDestinationFlyweight.decodeRouterDestinations(routes);
  }
}

/*
   0                   1                   2                   3
   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  |0|Frame Type |U|M|E|B|A|T|   |    Major Ver    |     Min Ver   |
  +-----------------------------+-----------------+---------------+
  |                          HMAC Token                           |
  +---------------------------------------------------------------+
  |0|                                                             |
  +-+                       From Access Key                       +
  |                                                               |
  +---------------------------------------------------------------+
  |0|                                                             |
  +-+                    From Destination Id                      +
  |                                                               |
  +---------------------------------------------------------------+
  |            User Metadata Length              |
  +----------------------------------------------+----------------+
  |                                                               |
  +                         Namespace Id                          +
  |                                                               |
  +---------------------------------------------------------------+
  |                                                               |
  +                           Class Id                            +
  |                                                               |
  +---------------------------------------------------------------+
  |                                                               |
  +                          Method Id                            +
  |                                                               |
  +---------------------------------------------------------------+
  ...
  +-------------+
  |F|Route Type |
  +-------------+-------------------------------------------------+
  |0|                                                             |
  +-+                        To Account Id                        +
  |                                                               |
  +---------------------------------------------------------------+
  |0|                                                             |
  +-+                      To Destination Id                      +
  |                                                               |
  +---------------------------------------------------------------+
  |F|                                                             |
  +-+                        To Group Id                          +
  |                                                               |
  +---------------------------------------------------------------+
  ...
*/
