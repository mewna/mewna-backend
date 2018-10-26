package com.mewna.data;

import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
    
    public static Webhook fromJson(final JsonObject data) {
        return new Webhook(data.getString("channel"), data.getString("guild"), data.getString("id"),
                data.getString("secret"));
    }
}
