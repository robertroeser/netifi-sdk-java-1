package io.netifi.sdk.annotations;

import java.lang.annotation.*;

/**
 *
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@NetifiAnnotation
public @interface FireForget {
    String serializer() default "";
}
