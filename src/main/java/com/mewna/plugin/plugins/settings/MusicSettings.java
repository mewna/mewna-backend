package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginBehaviour;
import com.mewna.plugin.plugins.PluginMusic;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_music")
@GIndex("id")
@SuppressWarnings("unused")
public class MusicSettings implements PluginSettings {
    @PrimaryKey
    private String id;
    private Map<String, CommandSettings> commandSettings;
    
    public MusicSettings(final String id) {
        this.id = id;
        commandSettings = generateCommandSettings(PluginMusic.class);
    }
    
    @Override
    public PluginSettings refreshCommands() {
        final Map<String, CommandSettings> oldSettings = new HashMap<>(commandSettings);
        final Map<String, CommandSettings> newSettings = generateCommandSettings(PluginMusic.class);
        newSettings.putAll(oldSettings);
        commandSettings.putAll(newSettings);
        return this;
    }
    
    @Override
    public boolean validateSettings(final JSONObject data) {
        return true;
    }
    
    @Override
    public boolean updateSettings(final Database database, final JSONObject data) {
        this.commandSettings.putAll(commandSettingsFromJson(data));
        database.saveSettings(this);
        return true;
    }
}
