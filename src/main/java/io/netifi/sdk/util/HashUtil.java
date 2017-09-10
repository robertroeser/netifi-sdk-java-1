package io.netifi.sdk.util;

import net.openhft.hashing.LongHashFunction;

/** */
public final class HashUtil {
  private static final LongHashFunction xx = LongHashFunction.xx();

  private HashUtil() {}

  public static long hash(String input) {
    return Math.abs(xx.hashChars(input));
  }
}
