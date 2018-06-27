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
public class UserEvent extends BaseEvent {
    private final UserEventType type;
    private final Player player;
    private final JSONObject data;
    
    public UserEvent(final UserEventType type, final Player player, final JSONObject data) {
        super(EventType.USER_EVENT);
        this.type = type;
        this.player = player;
        this.data = data;
    }
    
    public enum UserEventType {
        GLOBAL_LEVEL("event.levels.global"),
        
        /**
         * TODO: Add Twitch account linking
         */
        TWITCH_STREAM("event.social.twitch"),
        
        BACKGROUND("event.background"),
        DESCRIPTION("event.description"),
        
        MONEY("event.money"),
        ;
        @Getter
        private final String eventId;
    
        UserEventType(final String eventId) {
            this.eventId = eventId;
        }
    }
}
