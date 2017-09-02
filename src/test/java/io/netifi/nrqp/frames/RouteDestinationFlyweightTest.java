package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/** */
public class RouteDestinationFlyweightTest {
  @Test
  public void testComputeLengthWithDestination() {
    int expected = 17;
    int length = RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE);
    int length1 = RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE, 10);

    Assert.assertEquals(expected, length);
    Assert.assertEquals(length, length1);
  }

  @Test(expected = IllegalStateException.class)
  public void testExpectErrorWhenRoutingByGroupAndNoGroupsPresent() {
    RouteDestinationFlyweight.computeLength(RouteType.STREAM_GROUP_ROUTE);
  }

  @Test
  public void testComputeLengthWithGroups() {
    int expected = 33;
    int length = RouteDestinationFlyweight.computeLength(RouteType.STREAM_GROUP_ROUTE, 3);
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testEncodeRouteByDestination() {
    Random rnd = ThreadLocalRandom.current();
    int length = RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE);
    boolean last = false;
    RouteType routeType = RouteType.STREAM_ID_ROUTE;
    long accountId = Math.abs(rnd.nextLong());
    long destinationId = Math.abs(rnd.nextLong());

    ByteBuf byteBuf = Unpooled.buffer(length);
    int encodedLength =
        RouteDestinationFlyweight.encodeRouteByDestination(
            byteBuf, routeType, accountId, destinationId);

    Assert.assertEquals(length, encodedLength);

    try {
      RouteDestinationFlyweight.groupIds(byteBuf);
      Assert.fail("should throw exception when trying to route by groups");
    } catch (IllegalStateException e) {

    }

    RouteType routeType1 = RouteDestinationFlyweight.routeType(byteBuf);
    Assert.assertEquals(routeType, routeType1);

    long accountId1 = RouteDestinationFlyweight.accountId(byteBuf);
    Assert.assertEquals(accountId, accountId1);

    long destinationId1 = RouteDestinationFlyweight.destinationId(byteBuf);
    Assert.assertEquals(destinationId, destinationId1);
  }

  @Test
  public void testEncodeRouteByGroup() {
    Random rnd = ThreadLocalRandom.current();
    long[] groupIds =
        new long[] {Math.abs(rnd.nextLong()), Math.abs(rnd.nextLong()), Math.abs(rnd.nextLong())};
    int length =
        RouteDestinationFlyweight.computeLength(
            RouteType.STREAM_GROUP_ROUTE, groupIds.length);
    boolean last = true;
    RouteType routeType = RouteType.STREAM_GROUP_ROUTE;
    long accountId = Math.abs(rnd.nextLong());

    ByteBuf byteBuf = Unpooled.buffer(length);
    int encodedLength =
        RouteDestinationFlyweight.encodeRouteByGroup(byteBuf, routeType, accountId, groupIds);

    Assert.assertEquals(length, encodedLength);

    try {
      RouteDestinationFlyweight.destinationId(byteBuf);
      Assert.fail("should throw exception when trying to route by destination");
    } catch (IllegalStateException e) {

    }

    RouteType routeType1 = RouteDestinationFlyweight.routeType(byteBuf);
    Assert.assertEquals(routeType, routeType1);

    long accountId1 = RouteDestinationFlyweight.accountId(byteBuf);
    Assert.assertEquals(accountId, accountId1);

    long[] groupIds1 = RouteDestinationFlyweight.groupIds(byteBuf);
    Assert.assertArrayEquals(groupIds, groupIds1);

  }
  
}
