package com.mewna.plugin.plugins;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.mewna.data.Webhook;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.twitch.TwitchFollowerEvent;
import com.mewna.plugin.event.plugin.twitch.TwitchStreamEndEvent;
import com.mewna.plugin.event.plugin.twitch.TwitchStreamStartEvent;
import com.mewna.plugin.event.plugin.twitch.TwitchStreamerEvent;
import com.mewna.plugin.plugins.settings.TwitchSettings;
import com.mewna.plugin.plugins.settings.TwitchSettings.TwitchStreamerConfig;
import net.dv8tion.jda.core.exceptions.HttpException;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * TODO: HEAVY caching...
 * <p>
 * mfw these all end up being (2N+1) queries
 * <p>
 * feelsbadman
 *
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Twitch", desc = "Get alerts when your favourite streamers go live.", settings = TwitchSettings.class)
public class PluginTwitch extends BasePlugin {
    private final Cache<String, WebhookClient> webhookCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .removalListener((RemovalListener<String, WebhookClient>) notification -> {
                if(notification.getValue() != null) {
                    notification.getValue().close();
                }
            })
            .build();
    
    @Event(EventType.TWITCH_STREAM_START)
    public void handleStreamStart(final TwitchStreamStartEvent event) {
        handleHook(event, "stream-start");
    }
    
    @Event(EventType.TWITCH_STREAM_END)
    public void handleStreamEnd(final TwitchStreamEndEvent event) {
        handleHook(event, "stream-end");
    }
    
    @Event(EventType.TWITCH_FOLLOWER)
    public void handleStreamerFollow(final TwitchFollowerEvent event) {
        handleHook(event, "stream-follow");
    }
    
    private void handleHook(final TwitchStreamerEvent event, final String mode) {
        // TODO: Detect when nobody subscribes to stream up OR down for unsubbing
        final String streamerId = event.getStreamer().getId();
        getMewna().getDatabase().getStore().sql("SELECT id FROM settings_twitch " +
                "WHERE data->'twitchStreamers' @> '[{\"id\": \"" + streamerId + "\"}]';", p -> {
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                final Collection<String> webhookGuilds = new ArrayList<>();
                while(resultSet.next()) {
                    webhookGuilds.add(resultSet.getString("id"));
                }
                if(!webhookGuilds.isEmpty()) {
                    webhookGuilds.forEach(guildId -> {
                        final TwitchSettings settings = getDatabase().getOrBaseSettings(TwitchSettings.class, guildId);
                        final Optional<TwitchStreamerConfig> maybeStreamer = settings.getTwitchStreamers().stream()
                                .filter(e -> e.getId().equals(streamerId)).findFirst();
                        if(maybeStreamer.isPresent()) {
                            final TwitchStreamerConfig streamerConfig = maybeStreamer.get();
                            boolean canHook = false;
                            switch(mode) {
                                case "stream-start": {
                                    canHook = streamerConfig.isStreamStartMessagesEnabled();
                                    break;
                                }
                                case "stream-end": {
                                    canHook = streamerConfig.isStreamEndMessagesEnabled();
                                    break;
                                }
                                case "follow": {
                                    canHook = streamerConfig.isFollowMessagesEnabled();
                                    break;
                                }
                            }
                            if(canHook) {
                                if(settings.getTwitchWebhookChannel() != null) {
                                    final Optional<Webhook> maybeHook = getDatabase().getWebhook(settings.getTwitchWebhookChannel());
                                    if(maybeHook.isPresent()) {
                                        final Webhook webhook = maybeHook.get();
                                        try {
                                            final WebhookClient client = webhookCache.get(webhook.getId(),
                                                    () -> new WebhookClientBuilder(Long.parseLong(webhook.getId()), webhook.getSecret())
                                                            .build());
                                            
                                            // TODO: Templating
                                            try {
                                                switch(mode) {
                                                    case "stream-start": {
                                                        client.send(streamerConfig.getStreamStartMessage());
                                                        break;
                                                    }
                                                    case "stream-end": {
                                                        client.send(streamerConfig.getStreamEndMessage());
                                                        break;
                                                    }
                                                    case "follow": {
                                                        client.send(streamerConfig.getFollowMessage());
                                                        break;
                                                    }
                                                }
                                            } catch(final HttpException e) {
                                                // I cannot believe I have to do this.
                                                final String message = e.getMessage();
                                                // Extracts HTTP response code from the error message, because APPARENTLY
                                                // it's not necessary to expose it here...
                                                final String code = message
                                                        .replace("Request returned failure ", "")
                                                        .trim().split(" ", 2)[0];
                                                if(code.equalsIgnoreCase("404")) {
                                                    // Bad hook, delet
                                                    getLogger().warn("Deleting bad webhook {} on {}", webhook.getId(), webhook.getChannel());
                                                    getDatabase().deleteWebhook(webhook.getChannel());
                                                }
                                            }
                                        } catch(final ExecutionException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }
}
