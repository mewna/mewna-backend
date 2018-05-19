package com.mewna.data;

import lombok.Value;

/**
 * Settings for an individual command
 *
 * @author amy
 * @since 5/8/18.
 */
@Value
@SuppressWarnings("WeakerAccess")
public class CommandSettings {
    private boolean enabled;
    
    public static CommandSettings base() {
        return new CommandSettings(true);
    }
}
