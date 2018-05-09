package com.mewna.plugin.event.message;

import com.mewna.cache.entity.Channel;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
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
        super(EventType.MESSAGE_DELETE);
        this.messageId = messageId;
        this.channel = channel;
    }
}
