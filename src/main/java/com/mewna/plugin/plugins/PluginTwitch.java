package com.mewna.plugin.plugins;

import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.twitch.TwitchFollowerEvent;
import com.mewna.plugin.event.plugin.twitch.TwitchStreamEndEvent;
import com.mewna.plugin.event.plugin.twitch.TwitchStreamStartEvent;
import com.mewna.plugin.plugins.settings.TwitchSettings;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Twitch", desc = "Get alerts when your favourite streamers go live.", settings = TwitchSettings.class)
public class PluginTwitch extends BasePlugin {
    @Event(EventType.TWITCH_STREAM_START)
    public void handleStreamStart(final TwitchStreamStartEvent event) {
    
    }
    
    @Event(EventType.TWITCH_STREAM_END)
    public void handleStreamEnd(final TwitchStreamEndEvent event) {
    
    }
    
    @Event(EventType.TWITCH_FOLLOWER)
    public void handleStreamerFollow(final TwitchFollowerEvent event) {
    
    }
}
