package com.mewna.plugin.plugins.settings;

import com.mewna.Mewna;
import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginTwitch;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import gg.amy.singyeong.QueryBuilder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.*;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_twitch")
@GIndex({"id", "partneredStreamers", "twitchStreamers"})
@SuppressWarnings("unused")
public class TwitchSettings implements PluginSettings {
    @PrimaryKey
    private String id;
    private Map<String, CommandSettings> commandSettings;
    private String twitchWebhookChannel;
    /**
     * Twitch streamers are "partnered" on a per-Discord basis, because ex. a
     * streamer may have their own server and/or a server with friends and/or
     * etc
     */
    private Set<String> partneredStreamers = new HashSet<>();
    
    private List<TwitchStreamerConfig> twitchStreamers = new ArrayList<>();
    
    public TwitchSettings(final String id) {
        this.id = id;
        commandSettings = generateCommandSettings(PluginTwitch.class);
    }
    
    @Override
    public PluginSettings refreshCommands() {
        final Map<String, CommandSettings> oldSettings = new HashMap<>(commandSettings);
        final Map<String, CommandSettings> newSettings = generateCommandSettings(PluginTwitch.class);
        newSettings.putAll(oldSettings);
        commandSettings.putAll(newSettings);
        return this;
    }
    
    @Override
    public boolean validateSettings(final JsonObject data) {
        for(final String key : data.fieldNames()) {
            switch(key) {
                case "twitchWebhookChannel": {
                    final String id = data.getString(key, null);
                    if(id != null) {
                        if(!id.matches("\\d{16,21}")) {
                            return false;
                        }
                    }
                    break;
                }
                case "twitchStreamers": {
                    final JsonArray streamers = data.getJsonArray(key, null);
                    if(streamers == null) {
                        return false;
                    }
                    for(final Object o : streamers) {
                        final JsonObject streamer = (JsonObject) o;
                        try {
                            // If this fails, it's bad JSON or some shit, so reject it
                            streamer.mapTo(TwitchStreamerConfig.class);
                        } catch(final Exception e) {
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
    public boolean updateSettings(final Database database, final JsonObject data) {
        if(data.getString("twitchWebhookChannel", null) != null) {
            twitchWebhookChannel = data.getString("twitchWebhookChannel");
        }
        final JsonArray streamersJson = data.getJsonArray("twitchStreamers");
        final Collection<TwitchStreamerConfig> streamers = new ArrayList<>();
        for(final Object o : streamersJson) {
            final JsonObject streamer = (JsonObject) o;
            try {
                // If this fails, it's bad JSON or some shit, but it shouldn't have passed the validation steps anyway
                final TwitchStreamerConfig twitchStreamer = streamer.mapTo(TwitchStreamerConfig.class);
                
                if(twitchStreamer.isFollowMessagesEnabled()) {
                    // This is **very intentionally** done like this
                    // Later on, this will be used, but for now, we
                    // wanna make sure that people can't try to sneek it in
                    //noinspection ConstantConditions,StatementWithEmptyBody
                    if(false) {
                        // Sub to follow messages
                    } else {
                        twitchStreamer.followMessagesEnabled = false;
                    }
                }
                streamers.add(twitchStreamer);
            } catch(final Exception e) {
                return false;
            }
        }

        // If a streamer is in the new list, and NOT in the old list, then we
        // need to subscribe to those notifications
        final Collection<TwitchStreamerConfig> copy = new ArrayList<>(streamers);
        
        
        copy.removeIf(e -> twitchStreamers.stream().anyMatch(f -> f.id.equalsIgnoreCase(e.id)));
        copy.forEach(streamer -> {
            Mewna.getInstance().singyeong().send("telepathy",
                    new QueryBuilder().build(),
                    new JsonObject().put("t", "TWITCH_SUBSCRIBE")
                            .put("d", new JsonObject().put("id", streamer.getId()).put("topic", "streams")));
        });
        
        // We don't try to unsubscribe from webhooks because that would honestly be a nightmare x-x

        twitchStreamers.clear();
        twitchStreamers.addAll(streamers);
        database.saveSettings(this);
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
        
        @Override
        public String toString() {
            return String.format("TwitchStreamerConfig(%s)", id);
        }
    }
}
