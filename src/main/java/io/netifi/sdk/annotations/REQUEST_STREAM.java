package io.netifi.sdk.annotations;

import io.netifi.sdk.serializer.Serializer;

import java.lang.annotation.*;

/**
 *
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface REQUEST_STREAM {
    Class<? extends Serializer> serializer();
}
