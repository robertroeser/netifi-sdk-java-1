package io.netifi.proteus.admin;

import io.netifi.proteus.admin.tracing.AdminTraceService;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;

@Ignore
public class NetifiAdminTest {
  @Test
  public void testAdminTraceService() {
    NetifiAdmin admin = NetifiAdmin
                            .builder()
                            .socketAddress(
                                InetSocketAddress.createUnresolved("127.0.0.1", 6001),
                                InetSocketAddress.createUnresolved("127.0.0.1", 6002),
                                InetSocketAddress.createUnresolved("127.0.0.1", 6003)
                            )
                            .build();

    AdminTraceService traceService = admin.adminTraceService();

    traceService.streamDataJson().take(10).doOnNext(System.out::println).blockLast();
  }
}
