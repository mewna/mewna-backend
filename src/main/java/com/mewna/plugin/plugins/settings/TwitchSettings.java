package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginTwitch;
import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Accessors;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder
@Table("settings_twitch")
@Index("id")
public class TwitchSettings implements PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    private String twitchWebhookChannel;
    private Set<String> twitchStreamers;
    
    public static TwitchSettings base(final String id) {
        final Map<String, CommandSettings> settings = new HashMap<>();
        PluginSettings.commandsOwnedByPlugin(PluginTwitch.class).forEach(e -> settings.put(e, CommandSettings.base()));
        return new TwitchSettings(id, settings, null, new HashSet<>());
    }
    
    @Override
    public boolean validate(final JSONObject data) {
        // TODO
        for(final String key : data.keySet()) {
            switch(key) {
                default: {
                    break;
                }
            }
        }
        return true;
    }
}
