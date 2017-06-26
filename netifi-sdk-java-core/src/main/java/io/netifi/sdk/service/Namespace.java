package io.netifi.sdk.service;

import java.lang.annotation.*;

/** */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Namespace {
    long id();
}
