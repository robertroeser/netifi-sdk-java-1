package io.netifi.auth;

/** */
public class DefaultSystemClock implements Clock {
  @Override
  public long getEpochTime() {
    return System.currentTimeMillis();
  }
}
