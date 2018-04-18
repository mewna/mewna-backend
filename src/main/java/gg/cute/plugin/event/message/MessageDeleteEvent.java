package gg.cute.plugin.event.message;

import gg.cute.cache.entity.Channel;
import gg.cute.plugin.event.BaseEvent;
import lombok.Getter;

/**
 * @author amy
 * @since 4/17/18.
 */
@Getter
public class MessageDeleteEvent extends BaseEvent {
    private final String messageId;
    private final Channel channel;
    
    public MessageDeleteEvent(final String messageId, final Channel channel) {
        super("MESSAGE_DELETE");
        this.messageId = messageId;
        this.channel = channel;
    }
}
