package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginMisc;
import com.mewna.plugin.plugins.PluginMusic;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

import java.util.Map;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_misc")
@GIndex("id")
@SuppressWarnings("unused")
public class MiscSettings implements PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    
    public MiscSettings(final String id) {
        this.id = id;
        commandSettings = generateCommandSettings(PluginMisc.class);
    }
    
    @Override
    public boolean validateSettings(final JSONObject data) {
        return true;
    }
    
    @Override
    public boolean updateSettings(final Database database, final JSONObject data) {
        commandSettings.putAll(commandSettingsFromJson(data));
        database.saveSettings(this);
        return true;
    }
}
