package com.mewna.plugin.event.plugin.twitch;

import com.mewna.plugin.event.EventType;
import lombok.Getter;

/**
 * @author amy
 * @since 6/20/18.
 */
@Getter
public class TwitchFollowerEvent extends TwitchStreamerEvent {
    private final TwitchStreamer from;
    private final TwitchStreamer streamer;
    
    public TwitchFollowerEvent(final TwitchStreamer from, final TwitchStreamer streamer) {
        super(EventType.TWITCH_FOLLOWER);
        this.from = from;
        this.streamer = streamer;
    }
}
