package com.mewna.plugin.event.message;

import com.mewna.cache.entity.Channel;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
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
        super(EventType.MESSAGE_DELETE_BULK);
        this.channel = channel;
        this.messageIds = messageIds;
    }
}
