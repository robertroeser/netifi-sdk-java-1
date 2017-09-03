package io.netifi.sdk.util;

/** Created by robertroeser on 9/3/17. */
public class GroupUtil {
  public static long[] toGroupIdArray(String group) {
    String[] split = group.split(".");
    long[] ids = new long[split.length];
    for (int i = 0; i < split.length; i++) {
      ids[i] = Long.valueOf(split[i]);
    }

    return ids;
  }
}
