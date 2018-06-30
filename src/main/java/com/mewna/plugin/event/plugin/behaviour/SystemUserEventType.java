package com.mewna.plugin.event.plugin.behaviour;

import lombok.Getter;

/**
 * @author amy
 * @since 6/30/18.
 */
public enum SystemUserEventType {
    GLOBAL_LEVEL("event.levels.global"),
    
    /**
     * TODO: Add Twitch account linking
     */
    TWITCH_STREAM("event.social.twitch"),
    
    BACKGROUND("event.account.background"),
    DESCRIPTION("event.account.description"),
    
    MONEY("event.money"),;
    @Getter
    private final String eventId;
    
    SystemUserEventType(final String eventId) {
        this.eventId = eventId;
    }
}
