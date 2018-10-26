package com.mewna.queue;

import io.vertx.core.json.JsonObject;
import lombok.Value;

/**
 * @author amy
 * @since 4/8/18.
 */
@Value
public class SocketEvent {
    private final String type;
    private final JsonObject data;
    private final long timestamp;
    private final int shard;
    private final int limit;
}
