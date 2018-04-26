package gg.cute.plugin.event.guild.member;

import gg.cute.cache.entity.Guild;
import gg.cute.cache.entity.Member;
import gg.cute.plugin.event.BaseEvent;
import gg.cute.plugin.event.EventType;
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
