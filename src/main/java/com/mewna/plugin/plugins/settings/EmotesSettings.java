package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginEmotes;
import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Builder;
import lombok.Getter;
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
@Accessors(chain = true)
@Builder(toBuilder = true)
@Table("settings_emote")
@Index("id")
public class EmotesSettings implements PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    
    public static EmotesSettings base(final String id) {
        final Map<String, CommandSettings> settings = new HashMap<>();
        PluginSettings.commandsOwnedByPlugin(PluginEmotes.class).forEach(e -> settings.put(e, CommandSettings.base()));
        return new EmotesSettings(id, settings);
    }
    
    @Override
    public boolean validateSettings(final JSONObject data) {
        return true;
    }
    
    @Override
    public boolean updateSettings(final Database database, final JSONObject data) {
        final EmotesSettingsBuilder builder = toBuilder();
        builder.commandSettings(commandSettingsFromJson(data));
        database.saveSettings(builder.build());
        return true;
    }
}
