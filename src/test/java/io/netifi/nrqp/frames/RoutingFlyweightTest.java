package io.netifi.nrqp.frames;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/** */
public class RoutingFlyweightTest {

  @Test
  public void testComputeLengthWithApiNoMetadataNoToken() {
    int expected = 64;
    int length = RoutingFlyweight.computeLength(true, false, false, 1, Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithApiMetadataNoToken() {
    int expected = 68;
    int length = RoutingFlyweight.computeLength(true, true, false, 1, Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithApiMetadataToken() {
    int expected = 72;
    int length = RoutingFlyweight.computeLength(true, true, true, 1, Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithNoApiMetadataNoToken() {
    int expected = 44;
    int length = RoutingFlyweight.computeLength(false, true, false, 1, Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithNoApiMetadataToken() {
    int expected = 48;
    int length = RoutingFlyweight.computeLength(false, true, true, 1, Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testComputeLengthWithNoApiNoMetadataToken() {
    int expected = 44;
    int length = RoutingFlyweight.computeLength(false, false, true, 1, Unpooled.buffer(12));
    Assert.assertEquals(expected, length);
  }

  @Test
  public void testEncodeWithApiCall() {
    int length1 = RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE);
    ByteBuf routeDestinationBuf = Unpooled.buffer(length1);
    RouteDestinationFlyweight.encodeRouteByDestination(
        routeDestinationBuf, true, RouteType.STREAM_ID_ROUTE, 1, 1);

    int length = RoutingFlyweight.computeLength(true, true, false, 1, routeDestinationBuf);
   
    System.out.println(length1);
   
    ByteBuf byteBuf = Unpooled.buffer(length);

    boolean apiCall = true;
    boolean hasToken = false;
    boolean hasMetadata = true;
    int token = 0;
    long fromAccessKey = 1;
    int fromDestinationId = 2;
    int userMetadataLength = 3;
    long namespaceId = 4;
    long classId = 5;
    long methodId = 6;

    int encodeLength =
        RoutingFlyweight.encode(
            byteBuf,
            apiCall,
            hasToken,
            hasMetadata,
            token,
            fromAccessKey,
            fromDestinationId,
            userMetadataLength,
            namespaceId,
            classId,
            methodId,
            0,
            routeDestinationBuf);

    Assert.assertEquals(length, encodeLength);
    
    System.out.println(encodeLength);
    
    Assert.assertEquals(RoutingFlyweight.accessKey(byteBuf), fromAccessKey);
    Assert.assertEquals(RoutingFlyweight.destinationId(byteBuf), fromDestinationId);
    Assert.assertEquals(RoutingFlyweight.userMetadataLength(byteBuf), userMetadataLength);
    Assert.assertEquals(RoutingFlyweight.namespaceId(byteBuf), namespaceId);
    Assert.assertEquals(RoutingFlyweight.classId(byteBuf), classId);
    Assert.assertEquals(RoutingFlyweight.methodId(byteBuf), methodId);

    List<ByteBuf> routes = RoutingFlyweight.routes(byteBuf);

    Assert.assertEquals(1, routes.size());

    ByteBuf routeDestinationBuf1 = routes.get(0);
  
    RouteType routeType = RouteDestinationFlyweight.routeType(routeDestinationBuf1);
    Assert.assertEquals(RouteType.STREAM_ID_ROUTE, routeType);
  }
  
  @Test
  public void testEncodeWithoutApiCall() {
    int length1 = RouteDestinationFlyweight.computeLength(RouteType.STREAM_ID_ROUTE);
    ByteBuf routeDestinationBuf = Unpooled.buffer(length1);
    RouteDestinationFlyweight.encodeRouteByDestination(
        routeDestinationBuf, true, RouteType.STREAM_ID_ROUTE, 1, 1);
    
    int length = RoutingFlyweight.computeLength(false, true, false, 1, routeDestinationBuf);
    
    System.out.println(length1);
    
    ByteBuf byteBuf = Unpooled.buffer(length);
    
    boolean apiCall = false;
    boolean hasToken = false;
    boolean hasMetadata = true;
    int token = 0;
    long fromAccessKey = 1;
    int fromDestinationId = 2;
    int userMetadataLength = 3;
    long namespaceId = 4;
    long classId = 5;
    long methodId = 6;
    
    int encodeLength =
        RoutingFlyweight.encode(
            byteBuf,
            apiCall,
            hasToken,
            hasMetadata,
            token,
            fromAccessKey,
            fromDestinationId,
            userMetadataLength,
            namespaceId,
            classId,
            methodId,
            0,
            routeDestinationBuf);
    
    Assert.assertEquals(length, encodeLength);
    
    System.out.println(encodeLength);
    
    Assert.assertEquals(RoutingFlyweight.accessKey(byteBuf), fromAccessKey);
    Assert.assertEquals(RoutingFlyweight.destinationId(byteBuf), fromDestinationId);
    Assert.assertEquals(RoutingFlyweight.userMetadataLength(byteBuf), userMetadataLength);
    
    List<ByteBuf> routes = RoutingFlyweight.routes(byteBuf);
    
    Assert.assertEquals(1, routes.size());
    
    ByteBuf routeDestinationBuf1 = routes.get(0);
    
    RouteType routeType = RouteDestinationFlyweight.routeType(routeDestinationBuf1);
    Assert.assertEquals(RouteType.STREAM_ID_ROUTE, routeType);
  }
}
