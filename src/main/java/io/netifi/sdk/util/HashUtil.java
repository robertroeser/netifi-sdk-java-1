package io.netifi.sdk.util;

import net.openhft.hashing.LongHashFunction;

/**
 *
 */
public class HashUtil {
    private static final LongHashFunction xx = LongHashFunction.xx();
    
    public static long hash(String input) {
        return xx.hashChars(input);
    }
}
