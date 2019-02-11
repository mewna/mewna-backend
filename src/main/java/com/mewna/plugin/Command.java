package com.mewna.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author amy
 * @since 4/8/18.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    String[] names();
    
    String desc();
    
    String[] usage();
    
    String[] examples();
    
    boolean owner() default false;
    
    boolean staff() default false;
    
    boolean aliased() default true;
}

