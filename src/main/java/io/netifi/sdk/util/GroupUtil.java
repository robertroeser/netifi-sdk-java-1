package io.netifi.sdk.util;

import net.openhft.hashing.LongHashFunction;

/** */
public class GroupUtil {
  private static final LongHashFunction xx = LongHashFunction.xx();

  public static long[] toGroupIdArray(String group) {
    String[] split = group.split("\\.");
    long[] ids = new long[split.length];
    for (int i = 0; i < split.length; i++) {
      ids[i] = Math.abs(xx.hashChars(split[i]));
    }
    return ids;
  }
}
