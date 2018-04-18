package gg.cute.plugin.event.message;

import gg.cute.cache.entity.Channel;
import gg.cute.cache.entity.Guild;
import gg.cute.cache.entity.User;
import gg.cute.plugin.event.BaseEvent;
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
        super("MESSAGE_CREATE");
        this.author = author;
        this.channel = channel;
        this.guild = guild;
        this.mentions = mentions;
        this.content = content;
        this.everyoneMentioned = everyoneMentioned;
    }
}
