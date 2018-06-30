package com.mewna.plugin.event.plugin.behaviour;

import com.mewna.data.Player;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;
import org.json.JSONObject;

/**
 * @author amy
 * @since 6/24/18.
 */
@Getter
public class PlayerEvent extends BaseEvent {
    private final SystemUserEventType type;
    private final Player player;
    private final JSONObject data;
    
    public PlayerEvent(final SystemUserEventType type, final Player player, final JSONObject data) {
        super(EventType.PLAYER_EVENT);
        this.type = type;
        this.player = player;
        this.data = data;
    }
}
