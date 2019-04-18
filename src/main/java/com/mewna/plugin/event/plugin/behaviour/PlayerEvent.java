package com.mewna.plugin.event.plugin.behaviour;

import com.mewna.data.Player;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

/**
 * @author amy
 * @since 6/24/18.
 */
@Getter
public class PlayerEvent extends BaseEvent {
    private final SystemEventType type;
    private final Player player;
    private final JsonObject data;
    
    public PlayerEvent(final SystemEventType type, final Player player, final JsonObject data) {
        super(EventType.PLAYER_EVENT);
        this.type = type;
        this.player = player;
        this.data = data;
    }
}
