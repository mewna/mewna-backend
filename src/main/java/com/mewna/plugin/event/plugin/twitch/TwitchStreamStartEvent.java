package com.mewna.plugin.event.plugin.twitch;

import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;

/**
 * @author amy
 * @since 6/20/18.
 */
@Getter
public class TwitchStreamStartEvent extends BaseEvent {
    private final TwitchStreamer streamer;
    private final TwitchStreamData streamData;
    
    public TwitchStreamStartEvent(final TwitchStreamer streamer, final TwitchStreamData streamData) {
        super(EventType.TWITCH_STREAM_START);
        this.streamer = streamer;
        this.streamData = streamData;
    }
}
