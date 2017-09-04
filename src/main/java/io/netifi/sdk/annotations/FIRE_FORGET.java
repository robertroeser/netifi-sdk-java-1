package io.netifi.sdk.annotations;

import java.lang.annotation.*;

/**
 *
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FIRE_FORGET {
    String serializer();
}
