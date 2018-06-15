package com.mewna.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONObject;

/**
 * @author amy
 * @since 6/13/18.
 */
@Getter
@AllArgsConstructor
public class Webhook {
    private final String channel;
    private final String guild;
    private final String id;
    private final String secret;
    
    public static Webhook fromJson(final JSONObject data) {
        return new Webhook(data.getString("channel"), data.getString("guild"), data.getString("id"),
                data.getString("secret"));
    }
}
