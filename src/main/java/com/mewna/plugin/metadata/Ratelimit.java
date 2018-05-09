package com.mewna.plugin.metadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.mewna.plugin.metadata.RatelimitType.GUILD;

/**
 * @author amy
 * @since 4/15/18.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ratelimit {
    RatelimitType type() default GUILD;
    
    /**
     * @return How long, <strong>in seconds</strong>, the ratelimit is.
     */
    long time();
}
