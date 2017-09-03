package io.netifi.sdk.util;

import io.netifi.sdk.annotations.FIRE_FORGET;

import java.lang.annotation.Annotation;

/**
 *
 */
public class ClassUtil {
    public static boolean isNetifiRoutingAnnotation(Annotation annotation) {
        return true;
    }
}
