package io.netifi.sdk;

import io.netifi.auth.SessionUtil;
import io.netifi.nrqp.frames.RouteDestinationFlyweight;
import io.netifi.nrqp.frames.RouteType;
import io.netifi.nrqp.frames.RoutingFlyweight;
import io.netifi.sdk.annotations.FireForget;
import io.netifi.sdk.annotations.RequestChannel;
import io.netifi.sdk.annotations.RequestResponse;
import io.netifi.sdk.annotations.RequestStream;
import io.netifi.sdk.rs.RSocketBarrier;
import io.netifi.sdk.serializer.Serializer;
import io.netifi.sdk.serializer.Serializers;
import io.netifi.sdk.util.ClassUtil;
import io.netifi.sdk.util.TimebasedIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.reactivex.Flowable;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.util.PayloadImpl;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static io.netifi.sdk.util.HashUtil.hash;

/** */
class NetifiInvocationHandler implements InvocationHandler {
    private static final Logger logger = LoggerFactory.getLogger(NetifiInvocationHandler.class);
    
    private final RSocketBarrier barrier;
    
    private final long fromAccountId;
    
    private final String fromGroup;
    
    private final String fromDestination;
    
    private final long accountId;
    
    private final String group;
    
    private final String destination;
    
    private final TimebasedIdGenerator generator;
    
    private final SessionUtil sessionUtil;
    
    private final AtomicReference<AtomicLong> currentSessionCounter;
    
    private final AtomicReference<byte[]> currentSessionToken;
    
    public NetifiInvocationHandler(
                                      RSocketBarrier barrier,
                                      long accountId,
                                      String group,
                                      String destination,
                                      long fromAccountId,
                                      String fromGroup,
                                      String fromDestination,
                                      TimebasedIdGenerator generator,
                                      SessionUtil sessionUtil,
                                      AtomicReference<AtomicLong> currentSessionCounter,
                                      AtomicReference<byte[]> currentSessionToken) {
        this.barrier = barrier;
        this.accountId = accountId;
        this.group = group;
        this.destination = destination;
        this.generator = generator;
        this.fromAccountId = fromAccountId;
        this.fromGroup = fromGroup;
        this.fromDestination = fromDestination;
        this.sessionUtil = sessionUtil;
        this.currentSessionCounter = currentSessionCounter;
        this.currentSessionToken = currentSessionToken;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            
            Class<?> declaringClass = method.getDeclaringClass();
            long namespaceId = hash(declaringClass.getPackage().getName());
            long classId = hash(declaringClass.getName());
            long methodId = hash(method.getName());
            Type type = method.getGenericReturnType();
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            Class<?> returnType = (Class<?>) typeArguments[0];
            
            if (!method.getReturnType().isAssignableFrom(Flowable.class)) {
                throw new IllegalStateException("method must return " + Flowable.class.getCanonicalName());
            }
            
            Annotation[] annotations = method.getDeclaredAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation instanceof FireForget) {
                    FireForget fireforget = (FireForget) annotation;
                    Object arg = args != null ? args[0] : null;
                    Serializer<?> requestSerializer =
                        arg != null ? Serializers.getSerializer(fireforget.serializer(), arg) : null;
                    
                    return barrier
                               .getRSocket()
                               .flatMap(
                                   rSocket -> {
                                       long seqId = generator.nextId();
                                
                                       if (logger.isTraceEnabled()) {
                                           logger.trace(
                                               "{} - invoking \nnamespaceId -> {},\nclassId -> {},\nmethodId -> {}\nfor method {}, and class {}",
                                               seqId,
                                               namespaceId,
                                               classId,
                                               methodId,
                                               method,
                                               method.getDeclaringClass().getName());
                                       }
                                
                                       ByteBuf route;
                                
                                       if (destination != null && !destination.equals("")) {
                                           int length =
                                               RouteDestinationFlyweight.computeLength(
                                                   RouteType.STREAM_ID_ROUTE, destination, group);
                                           route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                           RouteDestinationFlyweight.encodeRouteByDestination(
                                               route, RouteType.STREAM_ID_ROUTE, accountId, destination, group);
                                       } else {
                                           int length =
                                               RouteDestinationFlyweight.computeLength(
                                                   RouteType.STREAM_GROUP_ROUTE, group);
                                           route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                           RouteDestinationFlyweight.encodeRouteByGroup(
                                               route, RouteType.STREAM_GROUP_ROUTE, accountId, group);
                                       }
                                
                                       int length =
                                           RoutingFlyweight.computeLength(true, false, true, fromDestination, route);
                                
                                       ByteBuffer data =
                                           arg != null ? requestSerializer.serialize(arg) : Frame.NULL_BYTEBUFFER;
                                
                                       int requestToken = sessionUtil.generateRequestToken(currentSessionToken.get(), data, currentSessionCounter.get().incrementAndGet());
                                
                                       ByteBuf metadata = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                       RoutingFlyweight.encode(
                                           metadata,
                                           true,
                                           true,
                                           false,
                                           requestToken,
                                           fromAccountId,
                                           fromDestination,
                                           0,
                                           namespaceId,
                                           classId,
                                           methodId,
                                           seqId,
                                           route);
                                       byte[] bytes = new byte[metadata.capacity()];
                                       metadata.getBytes(0, bytes);
                                       PayloadImpl payload = new PayloadImpl(data, ByteBuffer.wrap(bytes));
                                
                                       return rSocket.fireAndForget(payload);
                                   });
                } else if (annotation instanceof RequestChannel) {
                    RequestChannel requestchannel = (RequestChannel) annotation;
                    Object arg = args != null ? args[0] : null;
                    
                    if (args == null) {
                        throw new IllegalStateException("request channel must have an argument");
                    }
                    
                    Class<?> requestType = ClassUtil.getParametrizedClass(arg.getClass());
                    Serializer<?> requestSerializer =
                        Serializers.getSerializer(requestchannel.serializer(), requestType);
                    Serializer<?> responseSerializer =
                        Serializers.getSerializer(requestchannel.serializer(), returnType);
                    
                    return barrier
                               .getRSocket()
                               .flatMap(
                                   rSocket -> {
                                       Flowable<Payload> map =
                                           Flowable.fromPublisher((Publisher) arg)
                                               .map(
                                                   o -> {
                                                       long seqId = generator.nextId();
                                                
                                                       if (logger.isTraceEnabled()) {
                                                           logger.trace(
                                                               "{} - invoking \nnamespaceId -> {},\nclassId -> {},\nmethodId -> {}\nfor method {}, and class {}",
                                                               seqId,
                                                               namespaceId,
                                                               classId,
                                                               methodId,
                                                               method,
                                                               method.getDeclaringClass().getName());
                                                       }
                                                
                                                       ByteBuf route;
                                                       if (destination != null && !destination.equals("")) {
                                                           int length =
                                                               RouteDestinationFlyweight.computeLength(
                                                                   RouteType.STREAM_ID_ROUTE, destination, group);
                                                           route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                                           RouteDestinationFlyweight.encodeRouteByDestination(
                                                               route,
                                                               RouteType.STREAM_ID_ROUTE,
                                                               accountId,
                                                               destination,
                                                               group);
                                                       } else {
                                                           int length =
                                                               RouteDestinationFlyweight.computeLength(
                                                                   RouteType.STREAM_GROUP_ROUTE, group);
                                                           route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                                           RouteDestinationFlyweight.encodeRouteByGroup(
                                                               route, RouteType.STREAM_GROUP_ROUTE, accountId, group);
                                                       }
                                                
                                                       int length =
                                                           RoutingFlyweight.computeLength(
                                                               true, false, false, fromDestination, route);
                                                
                                                
                                                       ByteBuffer data = requestSerializer.serialize(o);
                                                       int requestToken = sessionUtil.generateRequestToken(currentSessionToken.get(), data, currentSessionCounter.get().incrementAndGet());
                                                
                                                       ByteBuf metadata =
                                                           PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                                       RoutingFlyweight.encode(
                                                           metadata,
                                                           true,
                                                           true,
                                                           false,
                                                           requestToken,
                                                           fromAccountId,
                                                           fromDestination,
                                                           0,
                                                           namespaceId,
                                                           classId,
                                                           methodId,
                                                           seqId,
                                                           route);
                                                
                                                       ByteBuffer buffer =
                                                           ByteBuffer.allocateDirect(metadata.capacity());
                                                       metadata.getBytes(0, buffer);
                                                
                                                       return new PayloadImpl(data, buffer);
                                                   });
                                
                                       return rSocket
                                                  .requestChannel(map)
                                                  .map(
                                                      payload -> {
                                                          ByteBuffer data = payload.getData();
                                                          return responseSerializer.deserialize(data);
                                                      });
                                   });
                    
                } else if (annotation instanceof RequestResponse) {
                    RequestResponse requestresponse = (RequestResponse) annotation;
                    Object arg = args != null ? args[0] : null;
                    Serializer<?> requestSerializer =
                        arg != null ? Serializers.getSerializer(requestresponse.serializer(), arg) : null;
                    Serializer<?> responseSerializer =
                        Serializers.getSerializer(requestresponse.serializer(), returnType);
                    
                    return barrier
                               .getRSocket()
                               .flatMap(
                                   rSocket -> {
                                       ByteBuf route;
                                       long seqId = generator.nextId();
                                
                                       if (logger.isTraceEnabled()) {
                                           logger.trace(
                                               "{} - invoking \nnamespaceId -> {},\nclassId -> {},\nmethodId -> {}\nfor method {}, and class {}",
                                               seqId,
                                               namespaceId,
                                               classId,
                                               methodId,
                                               method,
                                               method.getDeclaringClass().getName());
                                       }
                                
                                       if (destination != null && !destination.equals("")) {
                                           int length =
                                               RouteDestinationFlyweight.computeLength(
                                                   RouteType.STREAM_ID_ROUTE, destination, group);
                                           route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                           RouteDestinationFlyweight.encodeRouteByDestination(
                                               route, RouteType.STREAM_ID_ROUTE, accountId, destination, group);
                                       } else {
                                           int length =
                                               RouteDestinationFlyweight.computeLength(
                                                   RouteType.STREAM_GROUP_ROUTE, group);
                                           route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                           RouteDestinationFlyweight.encodeRouteByGroup(
                                               route, RouteType.STREAM_GROUP_ROUTE, accountId, group);
                                       }
                                       
                                       int length =
                                           RoutingFlyweight.computeLength(true, false, true, fromDestination, route);
                                
                                       ByteBuffer data =
                                           arg != null ? requestSerializer.serialize(arg) : Frame.NULL_BYTEBUFFER;
    
                                       long count = currentSessionCounter.get().incrementAndGet();
                                       
                                       int requestToken = sessionUtil.generateRequestToken(currentSessionToken.get(), data, count);
                                       data.flip();
                                       logger.debug("{} - requestToken {}", seqId, requestToken);
                                       ByteBuf metadata = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                       RoutingFlyweight.encode(
                                           metadata,
                                           true,
                                           true,
                                           false,
                                           requestToken,
                                           fromAccountId,
                                           fromDestination,
                                           0,
                                           namespaceId,
                                           classId,
                                           methodId,
                                           seqId,
                                           route);
                                
                                       byte[] bytes = new byte[metadata.capacity()];
                                       metadata.getBytes(0, bytes);
                                       PayloadImpl payload = new PayloadImpl(data, ByteBuffer.wrap(bytes));
                                
                                       return rSocket
                                                  .requestResponse(payload)
                                                  .map(
                                                      payload1 -> {
                                                          ByteBuffer data1 = payload1.getData();
                                                          return responseSerializer.deserialize(data1);
                                                      });
                                   });
                    
                } else if (annotation instanceof RequestStream) {
                    RequestStream requeststream = (RequestStream) annotation;
                    Object arg = args != null ? args[0] : null;
                    Serializer<?> requestSerializer =
                        arg != null ? Serializers.getSerializer(requeststream.serializer(), arg) : null;
                    Serializer<?> responseSerializer =
                        Serializers.getSerializer(requeststream.serializer(), returnType);
                    
                    return barrier
                               .getRSocket()
                               .flatMap(
                                   rSocket -> {
                                       long seqId = generator.nextId();
                                
                                       if (logger.isTraceEnabled()) {
                                           logger.trace(
                                               "{} - invoking \nnamespaceId -> {},\nclassId -> {},\nmethodId -> {}\nfor method {}, and class {}",
                                               seqId,
                                               namespaceId,
                                               classId,
                                               methodId,
                                               method,
                                               method.getDeclaringClass().getName());
                                       }
                                
                                       ByteBuf route;
                                
                                       if (destination != null && !destination.equals("")) {
                                           int length =
                                               RouteDestinationFlyweight.computeLength(
                                                   RouteType.STREAM_ID_ROUTE, destination, group);
                                           route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                           RouteDestinationFlyweight.encodeRouteByDestination(
                                               route, RouteType.STREAM_ID_ROUTE, accountId, destination, group);
                                       } else {
                                           int length =
                                               RouteDestinationFlyweight.computeLength(
                                                   RouteType.STREAM_GROUP_ROUTE, group);
                                           route = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                           RouteDestinationFlyweight.encodeRouteByGroup(
                                               route, RouteType.STREAM_GROUP_ROUTE, accountId, group);
                                       }
                                
                                       int length =
                                           RoutingFlyweight.computeLength(true, false, true, fromDestination, route);
                                
                                       ByteBuffer data =
                                           arg != null ? requestSerializer.serialize(arg) : Frame.NULL_BYTEBUFFER;
                                       int requestToken = sessionUtil.generateRequestToken(currentSessionToken.get(), data, currentSessionCounter.get().incrementAndGet());
                                
                                       ByteBuf metadata = PooledByteBufAllocator.DEFAULT.directBuffer(length);
                                       RoutingFlyweight.encode(
                                           metadata,
                                           true,
                                           true,
                                           false,
                                           requestToken,
                                           fromAccountId,
                                           fromDestination,
                                           0,
                                           namespaceId,
                                           classId,
                                           methodId,
                                           seqId,
                                           route);
                                
                                       byte[] bytes = new byte[metadata.capacity()];
                                       metadata.getBytes(0, bytes);
                                       PayloadImpl payload = new PayloadImpl(data, ByteBuffer.wrap(bytes));
                                
                                       return rSocket
                                                  .requestStream(payload)
                                                  .map(
                                                      payload1 -> {
                                                          ByteBuffer data1 = payload1.getData();
                                                          return responseSerializer.deserialize(data1);
                                                      });
                                   });
                }
            }
            throw new IllegalStateException("no method found with Service annotation");
        } catch (Throwable t) {
            logger.error("error invoking method", t);
            return Flowable.error(t);
        }
    }
}
