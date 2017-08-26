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
    RouteDestinationFlyweight.computeLength(RouteType.CHANNEL_ACKED_GROUP_ROUTE);
  }

  @Test
  public void testComputeLengthWithGroups() {
    int expected = 33;
    int length = RouteDestinationFlyweight.computeLength(RouteType.CHANNEL_ACKED_GROUP_ROUTE, 3);
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
            byteBuf, last, routeType, accountId, destinationId);

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

    boolean last1 = RouteDestinationFlyweight.isLast(byteBuf);
    Assert.assertEquals(last, last1);
  }

  @Test
  public void testEncodeRouteByGroup() {
    Random rnd = ThreadLocalRandom.current();
    long[] groupIds =
        new long[] {Math.abs(rnd.nextLong()), Math.abs(rnd.nextLong()), Math.abs(rnd.nextLong())};
    int length =
        RouteDestinationFlyweight.computeLength(
            RouteType.CHANNEL_ACKED_GROUP_ROUTE, groupIds.length);
    boolean last = true;
    RouteType routeType = RouteType.CHANNEL_ACKED_GROUP_ROUTE;
    long accountId = Math.abs(rnd.nextLong());

    ByteBuf byteBuf = Unpooled.buffer(length);
    int encodedLength =
        RouteDestinationFlyweight.encodeRouteByGroup(byteBuf, last, routeType, accountId, groupIds);

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

    boolean last1 = RouteDestinationFlyweight.isLast(byteBuf);
    Assert.assertEquals(last, last1);
  }

  @Test
  public void testDecodeMultipleDestinationRoutes() {
    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    int length = RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE);
    int count = 9;

    ByteBuf routes = Unpooled.buffer(length * count);
    List<ByteBuf> routeList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      boolean last = i + 1 >= count;
      RouteType routeType = i % 2 == 0 ? RouteType.STREAM_ID_ROUTE : RouteType.CHANNEL_ID_ROUTE;
      long accountId = Math.abs(rnd.nextLong());
      long destinationId = Math.abs(rnd.nextLong());
      ByteBuf byteBuf = Unpooled.buffer(length);
      RouteDestinationFlyweight.encodeRouteByDestination(
          byteBuf, last, routeType, accountId, destinationId);
      routes.setBytes(i * length, byteBuf.array());
      routeList.add(byteBuf);
    }

    List<ByteBuf> decodedRoutes = RouteDestinationFlyweight.decodeRouterDestinations(routes);
    Assert.assertEquals(routeList.size(), decodedRoutes.size());

    for (int i = 0; i < routeList.size(); i++) {
      ByteBuf o1 = routeList.get(i);
      ByteBuf o2 = decodedRoutes.get(i);

      Assert.assertEquals(
          RouteDestinationFlyweight.accountId(o1), RouteDestinationFlyweight.accountId(o2));

      Assert.assertEquals(
          RouteDestinationFlyweight.routeType(o1), RouteDestinationFlyweight.routeType(o2));

      Assert.assertEquals(
          RouteDestinationFlyweight.destinationId(o1), RouteDestinationFlyweight.destinationId(o2));
    }
  }

  @Test
  public void testDecodeMultipleGroupRoutes() {
    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    int length = RouteDestinationFlyweight.computeLength(RouteType.CHANNEL_GROUP_ROUTE, 3);
    int count = 9;

    ByteBuf routes = Unpooled.buffer(length * count);
    List<ByteBuf> routeList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      boolean last = i + 1 >= count;
      RouteType routeType = RouteType.CHANNEL_GROUP_ROUTE;
      long accountId = Math.abs(rnd.nextLong());

      long[] groupIds =
          new long[] {Math.abs(rnd.nextLong()), Math.abs(rnd.nextLong()), Math.abs(rnd.nextLong())};

      ByteBuf byteBuf = Unpooled.buffer(length);
      RouteDestinationFlyweight.encodeRouteByGroup(byteBuf, last, routeType, accountId, groupIds);
      routes.setBytes(i * length, byteBuf.array());
      routeList.add(byteBuf);
    }

    List<ByteBuf> decodedRoutes = RouteDestinationFlyweight.decodeRouterDestinations(routes);
    Assert.assertEquals(routeList.size(), decodedRoutes.size());

    for (int i = 0; i < routeList.size(); i++) {
      ByteBuf o1 = routeList.get(i);
      ByteBuf o2 = decodedRoutes.get(i);

      Assert.assertEquals(
          RouteDestinationFlyweight.accountId(o1), RouteDestinationFlyweight.accountId(o2));

      Assert.assertEquals(
          RouteDestinationFlyweight.routeType(o1), RouteDestinationFlyweight.routeType(o2));

      long[] g1 = RouteDestinationFlyweight.groupIds(o1);
      long[] g2 = RouteDestinationFlyweight.groupIds(o2);

      Assert.assertArrayEquals(g1, g2);
    }
  }

  private ByteBuf encodeGroup(boolean last) {
    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    RouteType routeType = RouteType.CHANNEL_GROUP_ROUTE;
    int length = RouteDestinationFlyweight.computeLength(RouteType.CHANNEL_GROUP_ROUTE, 3);
    long accountId = Math.abs(rnd.nextLong());

    long[] groupIds =
        new long[] {Math.abs(rnd.nextLong()), Math.abs(rnd.nextLong()), Math.abs(rnd.nextLong())};

    ByteBuf byteBuf = Unpooled.buffer(length);
    RouteDestinationFlyweight.encodeRouteByGroup(byteBuf, last, routeType, accountId, groupIds);
    return byteBuf;
  }

  private ByteBuf encodeDestination(boolean last) {
    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    int length = RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE);
    RouteType routeType = RouteType.STREAM_ID_ROUTE;
    long accountId = Math.abs(rnd.nextLong());
    long destinationId = Math.abs(rnd.nextLong());
    ByteBuf byteBuf = Unpooled.buffer(length);
    RouteDestinationFlyweight.encodeRouteByDestination(
        byteBuf, last, routeType, accountId, destinationId);
    return byteBuf;
  }

  @Test
  public void testDecodeMultipleRoutes() {
    int count = 10;
    List<ByteBuf> routeList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      boolean last = i + 1 >= count;
      if (i % 2 == 0) {
        routeList.add(encodeDestination(last));
      } else {
        routeList.add(encodeGroup(last));
      }
    }
  
    int length = routeList
                .stream()
                .map(ByteBuf::capacity)
                .reduce(0, (a, b) -> a + b);
    
    ByteBuf routes = Unpooled.buffer(length);
  
    routeList
        .forEach(b -> routes.writeBytes(b.array()));
    
    List<ByteBuf> decodedRoutes = RouteDestinationFlyweight.decodeRouterDestinations(routes);
    for (int i = 0; i < count; i++) {
      if (i % 2 == 0) {
        ByteBuf o1 = routeList.get(i);
        ByteBuf o2 = decodedRoutes.get(i);
  
        Assert.assertEquals(
            RouteDestinationFlyweight.accountId(o1), RouteDestinationFlyweight.accountId(o2));
  
        Assert.assertEquals(
            RouteDestinationFlyweight.routeType(o1), RouteDestinationFlyweight.routeType(o2));
  
        Assert.assertEquals(
            RouteDestinationFlyweight.destinationId(o1), RouteDestinationFlyweight.destinationId(o2));
      } else {
        ByteBuf o1 = routeList.get(i);
        ByteBuf o2 = decodedRoutes.get(i);
  
        Assert.assertEquals(
            RouteDestinationFlyweight.accountId(o1), RouteDestinationFlyweight.accountId(o2));
  
        Assert.assertEquals(
            RouteDestinationFlyweight.routeType(o1), RouteDestinationFlyweight.routeType(o2));
  
        long[] g1 = RouteDestinationFlyweight.groupIds(o1);
        long[] g2 = RouteDestinationFlyweight.groupIds(o2);
  
        Assert.assertArrayEquals(g1, g2);
      }
    }
  }
}
