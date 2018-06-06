package com.mewna.nats;

import com.mewna.Mewna;
import io.nats.client.Nats;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import io.nats.streaming.SubscriptionOptions;
import lombok.Getter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("unused")
public class NatsServer {
    // TODO: Client ID needs to use container name; use metadata service to fetch this
    private final StreamingConnectionFactory connectionFactory = new StreamingConnectionFactory("mewna-nats", "mewna-discord-backend");
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Mewna mewna;
    
    private final ExecutorService pool = Executors.newCachedThreadPool();
    
    @Getter
    private StreamingConnection connection;
    
    public NatsServer(final Mewna mewna) {
        this.mewna = mewna;
    }
    
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    public void connect() {
        try {
            final String natsUrl = System.getenv("NATS_URL");
            logger.info("Connecting to NATS with: {}", natsUrl);
            connectionFactory.setNatsConnection(Nats.connect(natsUrl));
            connection = connectionFactory.createConnection();
            connection.subscribe("backend-event-queue", "backend-event-queue", m -> {
                final String message = new String(m.getData());
                try {
                    final JSONObject o = new JSONObject(message);
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
                }
            }, new SubscriptionOptions.Builder().durableName("mewna-backend-event-queue-durable").build());
            connection.subscribe("backend-event-broadcast", m -> {
                final String message = new String(m.getData());
                logger.info("Got broadcast: {}", message);
            });
        } catch(final IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        try {
            getConnection().publish(queue, event.toString().getBytes());
        } catch(final IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}