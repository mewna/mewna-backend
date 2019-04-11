package com.mewna.plugin.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.mewna.plugin.commands.annotations.RatelimitType.GLOBAL;

/**
 * @author amy
 * @since 4/15/18.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ratelimit {
    RatelimitType type() default GLOBAL;
    
    /**
     * @return How long, <strong>in seconds</strong>, the ratelimit is.
     */
    long time();
}
