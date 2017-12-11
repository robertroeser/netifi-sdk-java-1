package io.netifi.proteus.admin;

import io.netifi.proteus.admin.tracing.AdminTraceService;
import org.junit.Test;

public class NetifiAdminTest {
  @Test
  public void testAdminTraceService() {
    NetifiAdmin admin = NetifiAdmin.builder().host("127.0.0.1").port(8001).build();

    AdminTraceService traceService = admin.adminTraceService();

    traceService.streamDataJson().take(10).doOnNext(System.out::println).blockLast();
  }
}
