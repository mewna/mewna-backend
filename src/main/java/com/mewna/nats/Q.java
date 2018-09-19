package com.mewna.nats;

import com.mewna.Mewna;
import io.sentry.Sentry;
import lombok.Getter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("unused")
public class Q {
    private final JedisPoolConfig config = new JedisPoolConfig();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Mewna mewna;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    @Getter
    private JedisPool jedisPool;
    
    public Q(final Mewna mewna) {
        this.mewna = mewna;
    }
    
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    public void connect() {
        config.setMaxIdle(10);
        config.setMaxTotal(10);
        config.setMaxWaitMillis(500);
        jedisPool = new JedisPool(config, System.getenv("REDIS_HOST"));
        pool.execute(() -> {
            while(true) {
                // TODO: Exit loop somehow?
                try(final Jedis jedis = jedisPool.getResource()) {
                    final List<String> out = jedis.blpop(0, System.getenv("EVENT_QUEUE"));
                    out.forEach(str -> {
                        try {
                            final JSONObject o = new JSONObject(str);
                            final SocketEvent event;
                            if(o.has("shard") && !o.isNull("shard")) {
                                final JSONObject shard = o.getJSONObject("shard");
                                event = new SocketEvent(o.getString("t"), o.getJSONObject("d"), o.getLong("ts"),
                                        shard.getInt("id"), shard.getInt("limit"));
                            } else {
                                // If we have no shard data, fake it
                                event = new SocketEvent(o.getString("t"), o.getJSONObject("d"), o.getLong("ts"), -1, -1);
                            }
                            pool.execute(() -> mewna.getEventManager().handle(event));
                        } catch(final Exception e) {
                            logger.error("Caught error while processing socket message:");
                            e.printStackTrace();
                            Sentry.capture(e);
                        }
                    });
                }
            }
        });
    }
    
    public <T> void broadcastBackendEvent(final String type, final T data) {
        pushEvent("backend-event-broadcast", type, data);
    }
    
    public <T> void pushBackendEvent(final String type, final T data) {
        pushEvent("backend-event-queue", type, data);
    }
    
    public <T> void pushShardEvent(final String type, final T data) {
        pushEvent("discord-event-queue", type, data);
    }
    
    public <T> void pushAudioEvent(final String type, final T data) {
        pushEvent("audio-event-queue", type, data);
    }
    
    public <T> void pushTwitchEvent(final String type, final T data) {
        pushEvent("twitch-event-queue", type, data);
    }
    
    private <T> void pushEvent(final String queue, final String type, final T data) {
        final JSONObject event = new JSONObject().put("t", type).put("ts", System.currentTimeMillis()).put("d", data);
        if(jedisPool != null) {
            try(final Jedis jedis = jedisPool.getResource()) {
                jedis.rpush(System.getenv("EVENT_QUEUE"), event.toString());
            }
        }
    }
}