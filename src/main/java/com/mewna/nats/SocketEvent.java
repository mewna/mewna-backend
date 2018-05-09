package com.mewna.nats;

import lombok.Value;
import org.json.JSONObject;

/**
 * @author amy
 * @since 4/8/18.
 */
@Value
public class SocketEvent {
    private final String type;
    private final JSONObject data;
    private final long timestamp;
    private final int shard;
    private final int limit;
}
