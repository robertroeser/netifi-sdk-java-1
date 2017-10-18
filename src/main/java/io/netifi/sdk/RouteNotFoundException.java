package io.netifi.sdk;

public class RouteNotFoundException extends Exception {
  public RouteNotFoundException(int packageId) {
    super("not route found for pacakge " + packageId);
  }

  public RouteNotFoundException(int packageId, int serviceId) {
    super("not route found for pacakge " + packageId + " service " + serviceId);
  }

  public RouteNotFoundException(int packageId, int serviceId, int method) {
    super(
        "not route found for pacakge "
            + packageId
            + " service "
            + serviceId
            + " and method "
            + method);
  }
}
