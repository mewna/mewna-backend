package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginLogging;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@Accessors(chain = true)
@Table("settings_logging")
@GIndex("id")
@SuppressWarnings("unused")
public class LoggingSettings implements PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    
    public LoggingSettings(final String id) {
        this.id = id;
        commandSettings = generateCommandSettings(PluginLogging.class);
    }
    
    @Override
    public PluginSettings refreshCommands() {
        final Map<String, CommandSettings> oldSettings = new HashMap<>(commandSettings);
        final Map<String, CommandSettings> newSettings = generateCommandSettings(PluginLogging.class);
        newSettings.putAll(oldSettings);
        commandSettings.putAll(newSettings);
        return this;
    }
    
    @Override
    public boolean validateSettings(final JsonObject data) {
        // Someday this will be needed
        /*
        for(final String key : data.keySet()) {
            switch(key) {
                default: {
                    break;
                }
            }
        }
        */
        return true;
    }
    
    @Override
    public boolean updateSettings(final Database database, final JsonObject data) {
        return false;
    }
}
