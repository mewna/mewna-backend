package com.mewna.plugin.plugins;

import com.mewna.catnip.entity.message.MessageOptions;
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
import com.mewna.util.Templater;
import io.sentry.Sentry;

import java.sql.ResultSet;
import java.util.*;

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
    
    private Templater map(final TwitchStreamerEvent event) {
        final Map<String, String> data = new HashMap<>();
        
        data.put("link", "https://twitch.tv/" + event.getStreamer().getLogin());
        data.put("streamer.name", event.getStreamer().getDisplayName());
        if(event instanceof TwitchStreamStartEvent) {
            final TwitchStreamStartEvent e = (TwitchStreamStartEvent) event;
            data.put("stream.viewers", String.valueOf(e.getStreamData().getViewerCount()));
            data.put("stream.title", e.getStreamData().getTitle());
        } else if(event instanceof TwitchStreamEndEvent) {
            @SuppressWarnings("unused")
            final TwitchStreamEndEvent e = (TwitchStreamEndEvent) event;
        } else if(event instanceof TwitchFollowerEvent) {
            final TwitchFollowerEvent e = (TwitchFollowerEvent) event;
            data.put("follower.name", e.getFrom().getDisplayName());
        }
        
        return Templater.fromMap(data);
    }
    
    private void handleHook(final TwitchStreamerEvent event, final String mode) {
        // TODO: Detect when nobody subscribes to stream up OR down for unsubbing
        final String streamerId = event.getStreamer().getId();
        // Oh god...
        database().getStore().sql("SELECT id FROM settings_twitch " +
                "WHERE data->'twitchStreamers' @> '[{\"id\": \"" + streamerId + "\"}]';", p -> {
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                final Collection<String> webhookGuilds = new ArrayList<>();
                while(resultSet.next()) {
                    webhookGuilds.add(resultSet.getString("id"));
                }
                // TODO: Cache accesses
                /*
                webhookGuilds.removeIf(e -> {
                    final Guild guild = getMewna().getCache().getGuild(e);
                    return guild == null || guild.getId() == null;
                });
                */
                if(!webhookGuilds.isEmpty()) {
                    webhookGuilds.forEach(guildId -> {
                        final TwitchSettings settings = database().getOrBaseSettings(TwitchSettings.class, guildId);
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
                                    final Optional<Webhook> maybeHook = database().getWebhook(settings.getTwitchWebhookChannel());
                                    if(maybeHook.isPresent()) {
                                        final Webhook webhook = maybeHook.get();
                                        final Templater templater = map(event);
                                        final MessageOptions messageOptions = new MessageOptions();
                                        try {
                                            switch(mode) {
                                                case "stream-start": {
                                                    messageOptions.content(templater.render(streamerConfig.getStreamStartMessage()));
                                                    break;
                                                }
                                                case "stream-end": {
                                                    messageOptions.content(templater.render(streamerConfig.getStreamEndMessage()));
                                                    break;
                                                }
                                                case "follow": {
                                                    messageOptions.content(templater.render(streamerConfig.getFollowMessage()));
                                                    break;
                                                }
                                            }
                                            //noinspection ResultOfMethodCallIgnored
                                            catnip().rest().webhook().executeWebhook(webhook.getId(), webhook.getSecret(), messageOptions);
                                        } catch(final Exception e) {
                                            Sentry.capture(e);
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
