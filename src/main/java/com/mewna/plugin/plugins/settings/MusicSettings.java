package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginMusic;
import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
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
@Builder
@Table("settings_music")
@Index("id")
public class MusicSettings implements PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    
    public static MusicSettings base(final String id) {
        final Map<String, CommandSettings> settings = new HashMap<>();
        PluginSettings.commandsOwnedByPlugin(PluginMusic.class).forEach(e -> settings.put(e, CommandSettings.base()));
        return new MusicSettings(id, settings);
    }
}
