package com.mewna.plugin.event.plugin.twitch;

import com.mewna.plugin.event.BaseEvent;

/**
 * @author amy
 * @since 6/21/18.
 */
public abstract class TwitchStreamerEvent extends BaseEvent {
    public TwitchStreamerEvent(final String type) {
        super(type);
    }
    
    public abstract TwitchStreamer getStreamer();
}
