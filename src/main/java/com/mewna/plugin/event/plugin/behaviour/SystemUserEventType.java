package com.mewna.plugin.event.plugin.behaviour;

import lombok.Getter;

/**
 * @author amy
 * @since 6/30/18.
 */
public enum SystemUserEventType {
    // Account events
    GLOBAL_LEVEL("event.levels.global"),
    
    TWITCH_STREAM("event.social.twitch"),
    
    ACCOUNT_BACKGROUND("event.account.background"),
    ACCOUNT_DESCRIPTION("event.account.description"),
    ACCOUNT_DISPLAY_NAME("event.account.displayName"),
    
    MONEY("event.money"),
    
    // Guild events
    GUILD_NAME("event.guild.name"),
    GUILD_DESCRIPTION("event.guild.description"),
    GUILD_BACKGROUND("event.guild.background"),
    ;
    @Getter
    private final String eventId;
    
    SystemUserEventType(final String eventId) {
        this.eventId = eventId;
    }
}
