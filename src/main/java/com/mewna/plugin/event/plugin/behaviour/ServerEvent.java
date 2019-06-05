package com.mewna.plugin.event.plugin.behaviour;

import com.mewna.data.guild.Server;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

/**
 * @author amy
 * @since 4/16/19.
 */
@Getter
public class ServerEvent extends BaseEvent {
    private final SystemEventType type;
    private final Server server;
    private final JsonObject data;
    
    public ServerEvent(final SystemEventType type, final Server server, final JsonObject data) {
        super(EventType.SERVER_EVENT);
        this.type = type;
        this.server = server;
        this.data = data;
    }
}
