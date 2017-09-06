package io.netifi.sdk;

import io.netifi.sdk.annotations.RequestResponse;
import io.netifi.sdk.annotations.RequestStream;
import io.netifi.sdk.rs.RequestHandlerMetadata;
import io.netifi.sdk.serializer.Serializer;
import io.netifi.sdk.util.HashUtil;
import io.reactivex.Flowable;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

/** */
public class DefaultRequestHandlerRegistryTest {
  @Test(expected = IllegalStateException.class)
  public void testAssignable() {
    DefaultRequestHandlerRegistry registry = new DefaultRequestHandlerRegistry();
    DefaultTestService service = new DefaultTestService();
    registry.registerHandler(Runnable.class, service);
  }

  @Test(expected = IllegalStateException.class)
  public void testNotAnInterface() {
    DefaultRequestHandlerRegistry registry = new DefaultRequestHandlerRegistry();
    DefaultTestService service = new DefaultTestService();
    registry.registerHandler(service.getClass(), service);
  }

  @Test
  public void testRegisterClass() throws Exception {
    DefaultRequestHandlerRegistry registry = new DefaultRequestHandlerRegistry();
    DefaultTestService service = new DefaultTestService();
    registry.registerHandler(TestService.class, service);
    Map<String, RequestHandlerMetadata> cachedMethods = registry.cachedMethods;
    Assert.assertEquals(3, cachedMethods.size());

    Class<TestService> testServiceClass = TestService.class;
    long namespaceId = HashUtil.hash(testServiceClass.getPackage().getName());
    long classId = HashUtil.hash(testServiceClass.getName());
    long methodId = HashUtil.hash("test");

    RequestHandlerMetadata metadata =
        cachedMethods.get(namespaceId + ":" + classId + ":" + methodId);

    Assert.assertNotNull(metadata);

    Object object = metadata.getObject();
    Serializer<?> requestSerializer = metadata.getRequestSerializer();
    Serializer<?> responseSerializer = metadata.getResponseSerializer();

    Method method = metadata.getMethod();
    Flowable<String> result = (Flowable<String>) method.invoke(object, 1234);
    String s = result.blockingFirst();
    Assert.assertEquals(s, "1234");
    
  }

  public interface TestService {
    @RequestResponse
    Flowable<String> test(Integer integer);

    @RequestResponse
    Flowable<String> noArgs();

    @RequestStream
    Flowable<Integer> stream();
  }

  public class DefaultTestService implements TestService {
    @Override
    public Flowable<String> test(Integer integer) {
      return Flowable.just(String.valueOf(integer));
    }

    @Override
    public Flowable<String> noArgs() {
      return Flowable.just("no args");
    }

    @Override
    public Flowable<Integer> stream() {
      return Flowable.range(1, 10);
    }
  }
}
