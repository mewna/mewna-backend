package com.mewna.plugin.event.plugin.behaviour;

import lombok.Getter;

/**
 * @author amy
 * @since 6/30/18.
 */
public enum SystemEventType {
    // Account events
    GLOBAL_LEVEL("event.levels.global"),
    
    TWITCH_STREAM("event.social.twitch"),
    
    ACCOUNT_BACKGROUND("event.account.background"),
    ACCOUNT_DESCRIPTION("event.account.description"),
    ACCOUNT_DISPLAY_NAME("event.account.displayName"),
    
    MONEY("event.money"),
    
    // Server events
    SERVER_NAME("event.server.name"),
    SERVER_DESCRIPTION("event.server.description"),
    SERVER_BACKGROUND("event.server.background"),
    ;
    @Getter
    private final String eventId;
    
    SystemEventType(final String eventId) {
        this.eventId = eventId;
    }
}
