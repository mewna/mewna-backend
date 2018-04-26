package gg.cute.plugin.event.guild.member;

import gg.cute.cache.entity.Guild;
import gg.cute.cache.entity.User;
import gg.cute.plugin.event.BaseEvent;
import gg.cute.plugin.event.EventType;
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
