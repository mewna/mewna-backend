package com.mewna.cache;

import lombok.Getter;

/**
 * @author amy
 * @since 5/19/18.
 */
public enum ChannelType {
    GUILD_TEXT(0),
    DM(1),
    GUILD_VOICE(2),
    GROUP_DM(3),
    GUILD_CATEGORY(4);
    
    @Getter
    private final int type;
    
    ChannelType(final int type) {
        this.type = type;
    }
}
