package com.mewna.plugin;

import com.mewna.data.plugin.PluginSettings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author amy
 * @since 4/8/18.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Plugin {
    String name();
    
    String desc();
    
    boolean enabled() default true;
    
    Class<? extends PluginSettings> settings();
}
