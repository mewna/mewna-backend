package gg.cute.nats;

import gg.cute.Cute;
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
public class NatsServer {
    // TODO: Client ID needs to use container name; use Rancher metadata service
    private final StreamingConnectionFactory connectionFactory = new StreamingConnectionFactory("cute-nats", "cute-discord-backend");
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Cute cute;
    
    private final ExecutorService pool = Executors.newCachedThreadPool();
    
    @Getter
    private StreamingConnection connection;
    
    public NatsServer(final Cute cute) {
        this.cute = cute;
    }
    
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    public void connect() {
        try {
            final String natsUrl = System.getenv("NATS_URL");
            logger.info("Connecting to NATS with: {}", natsUrl);
            connectionFactory.setNatsConnection(Nats.connect(natsUrl));
            connection = connectionFactory.createConnection();
            connection.subscribe("backend-event-queue", m -> {
                final String message = new String(m.getData());
                try {
                    
                    final JSONObject o = new JSONObject(message);
                    final JSONObject shard = o.getJSONObject("shard");
                    final SocketEvent event = new SocketEvent(o.getString("t"), o.getJSONObject("d"), o.getLong("ts"),
                            shard.getInt("id"), shard.getInt("limit"));
                    pool.execute(() -> cute.getEventManager().handle(event));
                } catch(final Exception e) {
                    logger.error("Caught error while processing socket message:");
                    e.printStackTrace();
                }
            }, new SubscriptionOptions.Builder().durableName("cute-discord-incoming-durable").build());
        } catch(final IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public <T> void pushEvent(final String type, final T data) {
        final JSONObject event = new JSONObject().put("t", type).put("ts", System.currentTimeMillis()).put("d", data);
        try {
            getConnection().publish("backend-event-queue", event.toString().getBytes());
        } catch(final IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}