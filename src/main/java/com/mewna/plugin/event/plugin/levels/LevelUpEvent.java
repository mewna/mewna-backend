package com.mewna.plugin.event.plugin.levels;

import com.mewna.catnip.entity.channel.Channel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.user.User;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * @author amy
 * @since 5/25/18.
 */
@Getter
@Accessors(fluent = true)
public class LevelUpEvent extends BaseEvent {
    private final Guild guild;
    private final Channel channel;
    private final User user;
    private final Member member;
    private final long level;
    private final long xp;
    
    public LevelUpEvent(final Guild guild, final Channel channel, final User user, final Member member, final long level,
                        final long xp) {
        super(EventType.LEVEL_UP);
        this.guild = guild;
        this.channel = channel;
        this.user = user;
        this.member = member;
        this.level = level;
        this.xp = xp;
    }
}
