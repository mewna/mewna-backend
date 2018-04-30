package gg.cute;

import gg.cute.cache.DiscordCache;
import gg.cute.data.Database;
import gg.cute.event.EventManager;
import gg.cute.jda.RestJDA;
import gg.cute.nats.NatsServer;
import gg.cute.plugin.PluginManager;
import gg.cute.util.Ratelimiter;
import lombok.Getter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static spark.Spark.*;

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
        logger.info("Starting Cute backend...");
        eventManager.getCache().connect();
        database.init();
        pluginManager.init();
        startApiServer();
        nats = new NatsServer(this);
        nats.connect();
        logger.info("Finished starting!");
    }
    
    private void startApiServer() {
        logger.info("Starting API server...");
        port(Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("80")));
        path("/cache", () -> {
            get("/user/:id", (req, res) -> new JSONObject(getCache().getUser(req.params(":id"))));
            get("/guild/:id", (req, res) -> new JSONObject(getCache().getGuild(req.params(":id"))));
            get("/channel/:id", (req, res) -> new JSONObject(getCache().getChannel(req.params(":id"))));
            get("/role/:id", (req, res) -> new JSONObject(getCache().getRole(req.params(":id"))));
        });
    }
    
    public DiscordCache getCache() {
        return eventManager.getCache();
    }
}
