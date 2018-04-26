package gg.cute.plugin.impl.abilities;

import lombok.Getter;
import org.apache.commons.text.WordUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static gg.cute.plugin.impl.abilities.Action.*;

/**
 * @author amy
 * @since 4/25/18.
 */
@SuppressWarnings("unused")
public enum Trigger {
    MESSAGE_SEND(SEND_MESSAGE, DELETE_MESSAGE, GIVE_ROLE, TAKE_ROLE, GIVE_MONEY, TAKE_MONEY),
    
    LEVEL_UP(SEND_MESSAGE, GIVE_ROLE, TAKE_ROLE, GIVE_MONEY, TAKE_MONEY),
    
    ACHIEVEMENT_GET(SEND_MESSAGE, GIVE_ROLE, TAKE_ROLE, GIVE_MONEY, TAKE_MONEY),
    
    MEMBER_JOIN(SEND_MESSAGE, GIVE_ROLE, GIVE_MONEY),
    
    MEMBER_LEAVE(SEND_MESSAGE),
    
    CUSTOM_COMMAND(SEND_MESSAGE, DELETE_MESSAGE, GIVE_ROLE, TAKE_ROLE, GIVE_MONEY, TAKE_MONEY),
    
    TWITCH_STREAM_START(SEND_MESSAGE),
    BEAM_STREAM_START(SEND_MESSAGE),
    TWITTER_POST(SEND_MESSAGE),
    REDDIT_POST(SEND_MESSAGE),
    ;
    
    @Getter
    private final List<Action> allowedActions;
    
    Trigger(final Action... allowedActions) {
        this.allowedActions = new ArrayList<>(Arrays.asList(allowedActions));
    }
    
    public String getName() {
        return WordUtils.capitalizeFully(name().replace("_", " "));
    }
}
