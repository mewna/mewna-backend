package com.mewna.plugin.event.plugin.levels;

import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;

/**
 * @author amy
 * @since 5/25/18.
 */
@Getter
public class LevelUpEvent extends BaseEvent {
    private final Guild guild;
    private final Channel channel;
    private final User user;
    private final long level;
    private final long xp;
    
    public LevelUpEvent(final Guild guild, final Channel channel, final User user, final long level, final long xp) {
        super(EventType.LEVEL_UP);
        this.guild = guild;
        this.channel = channel;
        this.user = user;
        this.level = level;
        this.xp = xp;
    }
}
