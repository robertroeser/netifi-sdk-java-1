package io.netifi.sdk.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@NetifiAnnotation
public @interface Service {
    long accountId();
    String group();
}
