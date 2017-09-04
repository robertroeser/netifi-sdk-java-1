package io.netifi.sdk.annotations;

import java.lang.annotation.*;

/**
 *
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface REQUEST_RESPONSE {
    String serializer();
}
