package com.mewna.plugin.metadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author amy
 * @since 4/15/18.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Payment {
    long min();
    
    long max() default Long.MAX_VALUE;
    
    boolean fromFirstArg() default false;
}
