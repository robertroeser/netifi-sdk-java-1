package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.Assert;
import org.junit.Test;

/** */
public class RoutingFlyweightTest {

  @Test
  public void testEncodeNoMetadata() {
    String fromDestination = "dest";
    String group = "group";
    String toDestination = "toDest";

    int routeDestinationFlyweight =
        RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE, toDestination, group);
    ByteBuf routeDestinationBuf = Unpooled.buffer(routeDestinationFlyweight);
    RouteDestinationFlyweight.encodeRouteByDestination(
        routeDestinationBuf, RouteType.STREAM_ID_ROUTE, 1, toDestination, group);

    int length = RoutingFlyweight.computeLength(false, fromDestination, routeDestinationBuf);

    ByteBuf byteBuf = Unpooled.buffer(length);

    long fromAccessKey = 1;

    int encodeLength =
        RoutingFlyweight.encode(
            byteBuf, false, 0, fromAccessKey, fromDestination, 1, routeDestinationBuf);

    Assert.assertEquals(length, encodeLength);
    Assert.assertEquals(RoutingFlyweight.accessKey(byteBuf), fromAccessKey);
    Assert.assertEquals(RoutingFlyweight.destination(byteBuf), fromDestination);
  }

  @Test
  public void testEncodeWithMetadata() {
    String fromDestination = "dest";
    String group = "group";
    String toDestination = "toDest";
    byte[] metadata = new byte[1024];
    ThreadLocalRandom.current().nextBytes(metadata);
    ByteBuf wrappedMetadata = Unpooled.wrappedBuffer(metadata);

    int routeDestinationFlyweight =
        RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE, toDestination, group);
    ByteBuf routeDestinationBuf = Unpooled.buffer(routeDestinationFlyweight);
    RouteDestinationFlyweight.encodeRouteByDestination(
        routeDestinationBuf, RouteType.STREAM_ID_ROUTE, 1, toDestination, group);

    int length =
        RoutingFlyweight.computeLength(
            false, fromDestination, routeDestinationBuf, wrappedMetadata);

    ByteBuf byteBuf = Unpooled.buffer(length);

    long fromAccessKey = 1;

    int encodeLength =
        RoutingFlyweight.encode(
            byteBuf,
            false,
            0,
            fromAccessKey,
            fromDestination,
            1,
            routeDestinationBuf,
            wrappedMetadata);

    Assert.assertEquals(length, encodeLength);
    Assert.assertEquals(RoutingFlyweight.accessKey(byteBuf), fromAccessKey);
    Assert.assertEquals(RoutingFlyweight.destination(byteBuf), fromDestination);

    ByteBuf metadataFromByteBuf = RoutingFlyweight.wrappedMetadata(byteBuf);

    Assert.assertEquals(metadataFromByteBuf.capacity(), metadata.length);

    for (int i = 0; i < metadata.length; i++) {
      byte b1 = metadata[i];
      byte b2 = metadataFromByteBuf.getByte(i);
      Assert.assertEquals("byte at index " + i + " not equal ", b1, b2);
    }
  }

  @Test
  public void testEncodeWithMetadataAndToken() {
    String fromDestination = "dest";
    String group = "group";
    String toDestination = "toDest";
    byte[] metadata = new byte[1024];
    ThreadLocalRandom.current().nextBytes(metadata);
    ByteBuf wrappedMetadata = Unpooled.wrappedBuffer(metadata);

    int routeDestinationLength =
        RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE, toDestination, group);

    byte[] routeBytes = new byte[routeDestinationLength];
    ByteBuf routeDestinationBuf = Unpooled.wrappedBuffer(routeBytes);

    RouteDestinationFlyweight.encodeRouteByDestination(
        routeDestinationBuf, RouteType.STREAM_ID_ROUTE, Long.MAX_VALUE, toDestination, group);

    Assert.assertTrue(routeDestinationBuf.capacity() > 0);
    Assert.assertEquals(RouteDestinationFlyweight.accountId(routeDestinationBuf), Long.MAX_VALUE);

    int length =
        RoutingFlyweight.computeLength(true, fromDestination, routeDestinationBuf, wrappedMetadata);

    byte[] bytes = new byte[length];
    ByteBuf routingByteBuf = Unpooled.wrappedBuffer(bytes);
    long fromAccessKey = 1;

    int encodeLength =
        RoutingFlyweight.encode(
            routingByteBuf,
            true,
            1234,
            fromAccessKey,
            fromDestination,
            1,
            routeDestinationBuf,
            wrappedMetadata);

    Assert.assertEquals(length, encodeLength);
    Assert.assertEquals(RoutingFlyweight.accessKey(routingByteBuf), fromAccessKey);
    Assert.assertEquals(RoutingFlyweight.destination(routingByteBuf), fromDestination);

    ByteBuf routeFromByteBuf = RoutingFlyweight.route(routingByteBuf);

    Assert.assertTrue(routeFromByteBuf.capacity() > 0);
    Assert.assertEquals(RouteDestinationFlyweight.accountId(routeFromByteBuf), Long.MAX_VALUE);
    Assert.assertEquals(RouteDestinationFlyweight.destination(routeFromByteBuf), toDestination);
    Assert.assertEquals(RouteDestinationFlyweight.group(routeFromByteBuf), group);

    ByteBuf metadataFromByteBuf = RoutingFlyweight.wrappedMetadata(routingByteBuf);

    Assert.assertEquals(metadataFromByteBuf.capacity(), metadata.length);

    for (int i = 0; i < metadata.length; i++) {
      byte b1 = metadata[i];
      byte b2 = metadataFromByteBuf.getByte(i);
      Assert.assertEquals("byte at index " + i + " not equal ", b1, b2);
    }
  }
}
