package gg.cute.plugin.event.message;

import gg.cute.cache.entity.Channel;
import gg.cute.plugin.event.BaseEvent;
import lombok.Getter;

import java.util.List;

/**
 * @author amy
 * @since 4/17/18.
 */
@Getter
public class MessageDeleteBulkEvent extends BaseEvent {
    private final Channel channel;
    private final List<String> messageIds;
    
    public MessageDeleteBulkEvent(final Channel channel, final List<String> messageIds) {
        super("MESSAGE_DELETE_BULK");
        this.channel = channel;
        this.messageIds = messageIds;
    }
}
