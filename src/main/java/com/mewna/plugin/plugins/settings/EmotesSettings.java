package com.mewna.plugin.plugins.settings;

import com.mewna.data.plugin.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.plugin.PluginSettings;
import com.mewna.plugin.plugins.PluginEmotes;
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
 * @since 5/19/18.
 */
@SuppressWarnings("unused")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_emote")
@GIndex("id")
public class EmotesSettings implements PluginSettings {
    @PrimaryKey
    private String id;
    private Map<String, CommandSettings> commandSettings;
    
    public EmotesSettings(final String id) {
        this.id = id;
        commandSettings = generateCommandSettings(PluginEmotes.class);
    }
    
    @Override
    public PluginSettings refreshCommands() {
        final Map<String, CommandSettings> oldSettings = new HashMap<>(commandSettings);
        final Map<String, CommandSettings> newSettings = generateCommandSettings(PluginEmotes.class);
        newSettings.putAll(oldSettings);
        commandSettings.putAll(newSettings);
        return this;
    }
    
    @Override
    public boolean validateSettings(final JsonObject data) {
        return true;
    }
    
    @Override
    public boolean updateSettings(final Database database, final JsonObject data) {
        commandSettings.putAll(commandSettingsFromJson(data));
        database.saveSettings(this);
        return true;
    }
}
