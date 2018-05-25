package com.mewna.plugin.event.plugin.levels;

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
    private final User user;
    
    public LevelUpEvent(final Guild guild, final User user) {
        super(EventType.LEVEL_UP);
        this.guild = guild;
        this.user = user;
    }
}
