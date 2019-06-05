package com.mewna.data.plugin;

import lombok.*;

/**
 * Settings for an individual command
 *
 * @author amy
 * @since 5/8/18.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("WeakerAccess")
public class CommandSettings {
    private boolean enabled;
    
    public static CommandSettings base() {
        return new CommandSettings(true);
    }
}
