package io.netifi.sdk.auth;

import java.nio.ByteBuffer;

/** Generates a shared secret based on a string input. */
public abstract class SessionUtil {
  public static final SessionUtil instance() {
    return new DefaultSessionUtil();
  }

  public abstract byte[] generateSessionToken(byte[] key, ByteBuffer data, long count);

  public abstract int generateRequestToken(byte[] sessionToken, ByteBuffer message, long count);

  public abstract boolean validateMessage(
      byte[] sessionToken, ByteBuffer message, int requestToken, long count);

  public abstract long getThirtySecondsStepsFromEpoch();
}
