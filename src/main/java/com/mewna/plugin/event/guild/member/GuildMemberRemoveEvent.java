package com.mewna.plugin.event.guild.member;

import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;

/**
 * @author amy
 * @since 4/17/18.
 */
@Getter
public class GuildMemberRemoveEvent extends BaseEvent {
    private final Guild guild;
    private final User user;
    
    public GuildMemberRemoveEvent(final Guild guild, final User user) {
        super(EventType.GUILD_MEMBER_REMOVE);
        this.guild = guild;
        this.user = user;
    }
}
