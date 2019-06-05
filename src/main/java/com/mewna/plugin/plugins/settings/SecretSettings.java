package com.mewna.plugin.plugins.settings;

import com.mewna.data.plugin.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.plugin.PluginSettings;
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
 * @since 10/18/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_secret")
@GIndex("id")
@SuppressWarnings("unused")
public class SecretSettings implements PluginSettings {
    @PrimaryKey
    private String id;
    private Map<String, CommandSettings> commandSettings = new HashMap<>();
    
    public SecretSettings(final String id) {
        this.id = id;
    }
    
    @Override
    public PluginSettings refreshCommands() {
        commandSettings = new HashMap<>();
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
