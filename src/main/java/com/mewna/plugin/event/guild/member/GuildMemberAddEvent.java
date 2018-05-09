package com.mewna.plugin.event.guild.member;

import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.Member;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;

/**
 * @author amy
 * @since 4/17/18.
 */
@Getter
public class GuildMemberAddEvent extends BaseEvent {
    private final Guild guild;
    private final Member member;
    
    public GuildMemberAddEvent(final Guild guild, final Member member) {
        super(EventType.GUILD_MEMBER_ADD);
        this.guild = guild;
        this.member = member;
    }
}
