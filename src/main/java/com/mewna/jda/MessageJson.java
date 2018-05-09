package com.mewna.jda;

import lombok.Getter;
import org.json.JSONObject;

/**
 * @author amy
 * @since 4/8/18.
 */
public class MessageJson {
    @Getter
    private final JSONObject object;
    
    public MessageJson(JSONObject object) {
        this.object = object;
    }
}
