package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginBehaviour;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 6/23/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_behaviour")
@GIndex("id")
@SuppressWarnings("unused")
public class BehaviourSettings implements PluginSettings {
    @PrimaryKey
    private String id;
    private Map<String, CommandSettings> commandSettings;
    private String prefix;
    
    public BehaviourSettings(final String id) {
        this.id = id;
        commandSettings = generateCommandSettings(PluginBehaviour.class);
    }
    
    @Override
    public PluginSettings refreshCommands() {
        final Map<String, CommandSettings> oldSettings = new HashMap<>(commandSettings);
        final Map<String, CommandSettings> newSettings = generateCommandSettings(PluginBehaviour.class);
        newSettings.putAll(oldSettings);
        commandSettings.putAll(newSettings);
        return this;
    }
    
    @Override
    public boolean validateSettings(final JsonObject data) {
        if(data.containsKey("prefix")) {
            final String prefix = data.getString("prefix", null);
            if(prefix != null) {
                // I like it being explicit here
                //noinspection RedundantIfStatement
                if(prefix.length() > 16 || prefix.length() < 1) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean updateSettings(final Database database, final JsonObject data) {
        if(data.containsKey("prefix")) {
            final String prefix = data.getString("prefix", null);
            if(prefix != null) {
                this.prefix = prefix;
            }
        }
        database.saveSettings(this);
        
        return true;
    }
}
