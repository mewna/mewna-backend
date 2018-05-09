package com.mewna.plugin.event.message;

import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;

import java.util.List;

/**
 * @author amy
 * @since 4/16/18.
 */
@Getter
public class MessageCreateEvent extends BaseEvent {
    private final User author;
    private final Channel channel;
    private final Guild guild;
    private final List<User> mentions;
    // TODO: Role-mention support?
    private final String content;
    // TODO: Probably should support embeds too...
    private final boolean everyoneMentioned;
    // TODO: Attachment support?
    // TODO: Reaction support?
    
    public MessageCreateEvent(final User author, final Channel channel, final Guild guild,
                              final List<User> mentions, final String content, final boolean everyoneMentioned) {
        super(EventType.MESSAGE_CREATE);
        this.author = author;
        this.channel = channel;
        this.guild = guild;
        this.mentions = mentions;
        this.content = content;
        this.everyoneMentioned = everyoneMentioned;
    }
}
