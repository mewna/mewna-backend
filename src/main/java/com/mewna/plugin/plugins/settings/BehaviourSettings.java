package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginBehaviour;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 6/23/18.
 */
@Getter
@Setter
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_behaviour")
@GIndex("id")
@SuppressWarnings("unused")
public class BehaviourSettings implements PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
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
    public boolean validateSettings(final JSONObject data) {
        if(data.has("prefix")) {
            final String prefix = data.optString("prefix");
            if(prefix != null) {
                // I like it being explicit here
                //noinspection RedundantIfStatement
                if(prefix.length() > 16) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean updateSettings(final Database database, final JSONObject data) {
        if(data.has("prefix")) {
            final String prefix = data.optString("prefix");
            if(prefix != null) {
                this.prefix = prefix;
            }
        }
        database.saveSettings(this);
        
        return true;
    }
}
