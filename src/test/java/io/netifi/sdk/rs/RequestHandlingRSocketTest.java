package io.netifi.sdk.rs;

import io.netifi.nrqp.frames.RouteDestinationFlyweight;
import io.netifi.nrqp.frames.RouteType;
import io.netifi.nrqp.frames.RoutingFlyweight;
import io.netifi.sdk.RequestHandlerRegistry;
import io.netifi.sdk.serializer.JSONSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.Flowable;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.util.PayloadImpl;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/** */
public class RequestHandlingRSocketTest {
  @Test
  public void testRoutingToRequestResponse() throws Exception {
    ConcurrentHashMap<String, RequestHandlerMetadata> handlerMetadataMap =
        new ConcurrentHashMap<>();
    RequestHandlingRSocket handlingRSocket = new RequestHandlingRSocket(new RequestHandlerRegistry() {
      @Override
      public <T> void registerHandler(T t) {
    
      }
  
      @Override
      public RequestHandlerMetadata lookup(String name) {
        return handlerMetadataMap.get(name);
      }
    });
    int length = RouteDestinationFlyweight.computeLength(RouteType.STREAM_GROUP_ROUTE, 1);
    ByteBuf route = Unpooled.buffer(length);
    RouteDestinationFlyweight.encodeRouteByGroup(route, RouteType.STREAM_GROUP_ROUTE, 123, 1);
    int metadataLength = RoutingFlyweight.computeLength(true, false, false, route);
    ByteBuf metadata = Unpooled.buffer(metadataLength);
    RoutingFlyweight.encode(metadata, true, false, false, 0, 1, 12, 0, 123, 1234, 12345, 1, route);

    JSONSerializer<User> requestSerializer = new JSONSerializer<>(User.class);
    JSONSerializer<Integer> responseSerializer = new JSONSerializer<>(Integer.class);

    UserService userService = new UserService();
    Method method = UserService.class.getDeclaredMethod("handle", User.class);

    RequestHandlerMetadata handlerMetadata =
        new RequestHandlerMetadata(
            requestSerializer,
            responseSerializer,
            method,
            UserService.class,
            userService,
            123,
            1234,
            12345);

    handlerMetadataMap.put("123:1234:12345", handlerMetadata);

    User user = new User();
    user.setFirstName("bob");
    user.setLastName("magee");
    user.setTs(1);

    ByteBuffer data = requestSerializer.serialize(user);

    byte[] bytes = new byte[metadata.capacity()];
    metadata.getBytes(0, bytes);
    PayloadImpl payload = new PayloadImpl(data, ByteBuffer.wrap(bytes));

    Payload block = handlingRSocket.requestResponse(payload).block();

    Integer deserialize = responseSerializer.deserialize(block.getData());
    System.out.println(deserialize);
    Assert.assertEquals(deserialize.intValue(), 1);
  }
  
  @Test
  public void testRoutingToRequestResponseNoArgs() throws Exception {
    ConcurrentHashMap<String, RequestHandlerMetadata> handlerMetadataMap =
        new ConcurrentHashMap<>();
    RequestHandlingRSocket handlingRSocket = new RequestHandlingRSocket(new RequestHandlerRegistry() {
      @Override
      public <T> void registerHandler(T t) {
    
      }
  
      @Override
      public RequestHandlerMetadata lookup(String name) {
        return handlerMetadataMap.get(name);
      }
    });
    int length = RouteDestinationFlyweight.computeLength(RouteType.STREAM_GROUP_ROUTE, 1);
    ByteBuf route = Unpooled.buffer(length);
    RouteDestinationFlyweight.encodeRouteByGroup(route, RouteType.STREAM_GROUP_ROUTE, 123, 1);
    int metadataLength = RoutingFlyweight.computeLength(true, false, false, route);
    ByteBuf metadata = Unpooled.buffer(metadataLength);
    RoutingFlyweight.encode(metadata, true, false, false, 0, 1, 12, 0, 123, 1234, 12346, 1, route);
    
    JSONSerializer<Integer> responseSerializer = new JSONSerializer<>(Integer.class);
    
    UserService userService = new UserService();
    Method method = UserService.class.getDeclaredMethod("handleNoArgs");
    
    RequestHandlerMetadata handlerMetadata =
        new RequestHandlerMetadata(
                                      null,
                                      responseSerializer,
                                      method,
                                      UserService.class,
                                      userService,
                                      123,
                                      1234,
                                      12346);
    
    handlerMetadataMap.put("123:1234:12346", handlerMetadata);
    
    byte[] bytes = new byte[metadata.capacity()];
    metadata.getBytes(0, bytes);
    PayloadImpl payload = new PayloadImpl(Frame.NULL_BYTEBUFFER, ByteBuffer.wrap(bytes));
    
    Payload block = handlingRSocket.requestResponse(payload).block();
    
    Integer deserialize = responseSerializer.deserialize(block.getData());
    System.out.println(deserialize);
    Assert.assertNotNull(deserialize);
  }
  
  @Test
  public void testFireAndForgetNoArgs() throws Exception {
    ConcurrentHashMap<String, RequestHandlerMetadata> handlerMetadataMap =
        new ConcurrentHashMap<>();
    RequestHandlingRSocket handlingRSocket = new RequestHandlingRSocket(new RequestHandlerRegistry() {
      @Override
      public <T> void registerHandler(T t) {
    
      }
  
      @Override
      public RequestHandlerMetadata lookup(String name) {
        return handlerMetadataMap.get(name);
      }
    });
    int length = RouteDestinationFlyweight.computeLength(RouteType.STREAM_GROUP_ROUTE, 1);
    ByteBuf route = Unpooled.buffer(length);
    RouteDestinationFlyweight.encodeRouteByGroup(route, RouteType.STREAM_GROUP_ROUTE, 123, 1);
    int metadataLength = RoutingFlyweight.computeLength(true, false, false, route);
    ByteBuf metadata = Unpooled.buffer(metadataLength);
    RoutingFlyweight.encode(metadata, true, false, false, 0, 1, 12, 0, 123, 1234, 12346, 1, route);
    
    JSONSerializer<Integer> responseSerializer = new JSONSerializer<>(Integer.class);
    
    UserService userService = new UserService();
    Method method = UserService.class.getDeclaredMethod("ffNoArgs");
    
    RequestHandlerMetadata handlerMetadata =
        new RequestHandlerMetadata(
                                      null,
                                      responseSerializer,
                                      method,
                                      UserService.class,
                                      userService,
                                      123,
                                      1234,
                                      12346);
    
    handlerMetadataMap.put("123:1234:12346", handlerMetadata);
    
    byte[] bytes = new byte[metadata.capacity()];
    metadata.getBytes(0, bytes);
    PayloadImpl payload = new PayloadImpl(Frame.NULL_BYTEBUFFER, ByteBuffer.wrap(bytes));
    
    handlingRSocket.fireAndForget(payload).block();
  }
  
  private static class UserService {
    Flowable<Integer> handle(User user) {
      long ts = user.getTs();
      return Flowable.just((int) ts);
    }
    
    Flowable<Integer> handleNoArgs() {
      return Flowable.just((int) System.currentTimeMillis());
    }
  
    Flowable<Integer> stream() {
      return Flowable.range(1, 10);
    }
    
    Flowable<Void> ffNoArgs() {
      return Flowable.empty();
    }
  }

  private static class User {
    private String firstName;
    private String lastName;
    private long ts;

    public String getFirstName() {
      return firstName;
    }

    public void setFirstName(String firstName) {
      this.firstName = firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public void setLastName(String lastName) {
      this.lastName = lastName;
    }

    public long getTs() {
      return ts;
    }

    public void setTs(long ts) {
      this.ts = ts;
    }
  }
}
