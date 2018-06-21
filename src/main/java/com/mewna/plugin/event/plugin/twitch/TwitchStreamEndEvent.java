package com.mewna.plugin.event.plugin.twitch;

import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;

/**
 * @author amy
 * @since 6/20/18.
 */
@Getter
public class TwitchStreamEndEvent extends BaseEvent {
    private final TwitchStreamer streamer;
    
    public TwitchStreamEndEvent(final TwitchStreamer streamer) {
        super(EventType.TWITCH_STREAM_END);
        this.streamer = streamer;
    }
}
