package gg.cute;

import gg.cute.cache.DiscordCache;
import gg.cute.data.Database;
import gg.cute.event.EventManager;
import gg.cute.jda.RestJDA;
import gg.cute.nats.NatsServer;
import gg.cute.plugin.PluginManager;
import gg.cute.util.Ratelimiter;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amy
 * @since 4/8/18.
 */
public final class Cute {
    @Getter
    private final EventManager eventManager = new EventManager(this);
    @Getter
    private final PluginManager pluginManager = new PluginManager(this);
    @Getter
    private final RestJDA restJDA = new RestJDA(System.getenv("TOKEN"));
    @Getter
    private final Database database = new Database(this);
    @Getter
    private final Ratelimiter ratelimiter = new Ratelimiter(this);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private NatsServer nats;
    
    private Cute() {
    }
    
    public static void main(final String[] args) {
        new Cute().start();
    }
    
    private void start() {
        eventManager.getCache().connect();
        database.init();
        pluginManager.init();
        nats = new NatsServer(this);
        nats.connect();
        logger.info("Finished starting!");
    }
    
    public DiscordCache getCache() {
        return eventManager.getCache();
    }
}
