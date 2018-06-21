package com.mewna.plugin.event.plugin.twitch;

import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;

/**
 * @author amy
 * @since 6/20/18.
 */
@Getter
public class TwitchFollowerEvent extends BaseEvent {
    private final TwitchStreamer from;
    private final TwitchStreamer to;
    
    public TwitchFollowerEvent(final TwitchStreamer from, final TwitchStreamer to) {
        super(EventType.TWITCH_FOLLOWER);
        this.from = from;
        this.to = to;
    }
}
