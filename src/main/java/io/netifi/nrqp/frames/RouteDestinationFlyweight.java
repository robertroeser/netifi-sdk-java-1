package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/** */
public class RouteDestinationFlyweight {

  private static final long GROUP_LAST_MASK = 0x8000000000000000L;
  private static final long GROUP_REMOVE_FLAG = 0x7FFFFFFFFFFFFFFFL;
  private static final int LAST_MASK = 0b1000_0000;
  private static final int REMOVE_FLAG = 0b0111_1111;
  private static final int ROUTE_TYPE_SIZE = BitUtil.SIZE_OF_BYTE;
  private static final int ACCOUNT_ID_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int DESTINATION_ID_SIZE = BitUtil.SIZE_OF_LONG;
  private static final int GROUP_ID_SIZE = BitUtil.SIZE_OF_LONG;
  private static final IllegalStateException UNDEFINED_ROUTE_EXCEPTION =
      new IllegalStateException("undefined route");

  private RouteDestinationFlyweight() {}

  public static long accountId(ByteBuf byteBuf) {
    return byteBuf.getLong(ROUTE_TYPE_SIZE);
  }

  public static long destinationId(ByteBuf byteBuf) {
    RouteType routeType = routeType(byteBuf);
    if (!routeType.hasDestination()) {
      throw new IllegalStateException(
          "RouteType " + routeType + " does not contain a Destination Id");
    }
    return byteBuf.getLong(ROUTE_TYPE_SIZE + ACCOUNT_ID_SIZE);
  }

  public static RouteType routeType(ByteBuf byteBuf) {
    int id = byteBuf.getByte(0) & REMOVE_FLAG;
    return RouteType.from(id);
  }

  public static long[] groupIds(ByteBuf byteBuf) {
    RouteType routeType = routeType(byteBuf);
    if (!routeType.hasDestination()) {
      int offset = ROUTE_TYPE_SIZE + ACCOUNT_ID_SIZE;
      int remaining = (byteBuf.capacity() - offset) / GROUP_ID_SIZE;
      long[] groupIds = remaining == 0 ? BitUtil.EMPTY : new long[remaining];
      for (int i = 0; i < remaining; i++) {
        long groupId = byteBuf.getLong(offset);
        groupIds[i] = groupId & GROUP_REMOVE_FLAG;
        offset += GROUP_ID_SIZE;
      }
      return groupIds;
    } else {
      throw new IllegalStateException("RouteType " + routeType + " does not contain groups");
    }
  }

  public static int computeLength(RouteType routeType) {
    return computeLength(routeType, 0);
  }

  public static int computeLength(RouteType routeType, int numGroups) {
    if (numGroups == 0 && !routeType.hasDestination()) {
      throw new IllegalStateException("RouteType " + routeType + " expects groups present");
    }

    return ROUTE_TYPE_SIZE
        + ACCOUNT_ID_SIZE
        + (routeType.hasDestination() ? DESTINATION_ID_SIZE : 0)
        + (!routeType.hasDestination() ? GROUP_ID_SIZE * numGroups : 0);
  }

  public static int encodeRouteByDestination(
      ByteBuf byteBuf, RouteType routeType, long accountId, long destinationId, long... groupIds) {
    return encode(byteBuf, routeType, accountId, destinationId, groupIds);
  }

  public static int encodeRouteByGroup(
      ByteBuf byteBuf, RouteType routeType, long accountId, long... groupIds) {
    return encode(byteBuf, routeType, accountId, 0, groupIds);
  }

  private static int encode(
      ByteBuf byteBuf,
      RouteType routeType,
      long accountId,
      long destinationId,
      long... groupIds) {
    int offset = 0;
    int encodedType = routeType.getEncodedType();

    byteBuf.setByte(0, encodedType);
    offset += ROUTE_TYPE_SIZE;

    byteBuf.setLong(offset, accountId);
    offset += ACCOUNT_ID_SIZE;

    if (routeType.hasDestination()) {
      byteBuf.setLong(offset, destinationId);
      offset += DESTINATION_ID_SIZE;
    }

    if (!routeType.hasDestination()) {
      int groupIdsLength = groupIds.length;
      for (int i = 0; i < groupIdsLength; i++) {
        long groupId = groupIds[i];
        if (i + 1 >= groupIdsLength) {
          groupId |= GROUP_LAST_MASK;
        }
        byteBuf.setLong(offset, groupId);
        offset += GROUP_ID_SIZE;
      }
    }

    return offset;
  }

}
