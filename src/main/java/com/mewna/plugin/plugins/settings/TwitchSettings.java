package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginTwitch;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.*;
import lombok.experimental.Accessors;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder(toBuilder = true)
@Table("settings_twitch")
@GIndex({"id", "partneredStreamers", "twitchStreamers"})
@SuppressWarnings("unused")
public class TwitchSettings implements PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    private String twitchWebhookChannel;
    /**
     * Twitch streamers are "partnered" on a per-Discord basis, because ex. a
     * streamer may have their own server and/or a server with friends and/or
     * etc
     */
    private Set<String> partneredStreamers;
    
    private List<TwitchStreamerConfig> twitchStreamers;
    
    public static TwitchSettings base(final String id) {
        final Map<String, CommandSettings> settings = new HashMap<>();
        PluginSettings.commandsOwnedByPlugin(PluginTwitch.class).forEach(e -> settings.put(e, CommandSettings.base()));
        return new TwitchSettings(id, settings, null, new HashSet<>(), new ArrayList<>());
    }
    
    @Override
    public boolean validateSettings(final JSONObject data) {
        for(final String key : data.keySet()) {
            switch(key) {
                case "twitchWebhookChannel": {
                    final String id = data.optString(key, null);
                    if(id != null) {
                        if(!id.matches("\\d{16,21}")) {
                            return false;
                        }
                    }
                    break;
                }
                case "twitchStreamers": {
                    final JSONArray streamers = data.optJSONArray(key);
                    if(streamers == null) {
                        return false;
                    }
                    for(final Object o : streamers) {
                        final JSONObject streamer = (JSONObject) o;
                        try {
                            // If this fails, it's bad JSON or some shit, so reject it
                            MAPPER.readValue(streamer.toString(), TwitchStreamerConfig.class);
                        } catch(final IOException e) {
                            return false;
                        }
                    }
                    break;
                }
                default: {
                    break;
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean updateSettings(final Database database, final JSONObject data) {
        final TwitchSettingsBuilder builder = toBuilder();
        if(data.optString("twitchWebhookChannel") != null) {
            if(!data.isNull("twitchWebhookChannel")) {
                builder.twitchWebhookChannel(data.getString("twitchWebhookChannel"));
            }
        }
        final JSONArray streamersJson = data.getJSONArray("twitchStreamers");
        final List<TwitchStreamerConfig> streamers = new ArrayList<>();
        for(final Object o : streamersJson) {
            final JSONObject streamer = (JSONObject) o;
            try {
                // If this fails, it's bad JSON or some shit, but it shouldn't have passed the validation steps anyway
                final TwitchStreamerConfig twitchStreamer = MAPPER.readValue(streamer.toString(), TwitchStreamerConfig.class);
                streamers.add(twitchStreamer);
            } catch(final IOException e) {
                return false;
            }
        }
        builder.twitchStreamers(streamers);
        database.saveSettings(builder.build());
        return true;
    }
    
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("WeakerAccess")
    public static class TwitchStreamerConfig {
        private String id;
        private boolean streamStartMessagesEnabled;
        private String streamStartMessage;
        private boolean streamEndMessagesEnabled;
        private String streamEndMessage;
        private boolean followMessagesEnabled;
        private String followMessage;
    }
}
