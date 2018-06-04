package com.mewna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.cache.DiscordCache;
import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.Role;
import com.mewna.cache.entity.User;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.event.EventManager;
import com.mewna.jda.RestJDA;
import com.mewna.nats.NatsServer;
import com.mewna.plugin.CommandManager;
import com.mewna.plugin.PluginManager;
import com.mewna.plugin.util.TextureManager;
import com.mewna.util.Ratelimiter;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static spark.Spark.*;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("Singleton")
public final class Mewna {
    @SuppressWarnings("StaticVariableOfConcreteClass")
    private static final Mewna INSTANCE = new Mewna();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    @Getter
    private final EventManager eventManager = new EventManager(this);
    @Getter
    private final PluginManager pluginManager = new PluginManager(this);
    @Getter
    private final CommandManager commandManager = new CommandManager(this);
    @Getter
    private final RestJDA restJDA = new RestJDA(System.getenv("TOKEN"));
    @Getter
    private final Database database = new Database(this);
    @Getter
    private final Ratelimiter ratelimiter = new Ratelimiter(this);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private NatsServer nats;
    
    private Mewna() {
    }
    
    public static void main(final String[] args) {
        INSTANCE.start();
    }
    
    @SuppressWarnings("unused")
    public static Mewna getInstance() {
        return INSTANCE;
    }
    
    private void start() {
        logger.info("Starting Mewna backend...");
        TextureManager.preload();
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
            get("/user/:id", (req, res) -> {
                final User user = getCache().getUser(req.params(":id"));
                if(user != null) {
                    return new JSONObject(user);
                } else {
                    res.status(404);
                    return new JSONObject();
                }
            });
            get("/guild/:id", (req, res) -> {
                final Guild guild = getCache().getGuild(req.params(":id"));
                if(guild != null) {
                    return new JSONObject(guild);
                } else {
                    res.status(404);
                    return new JSONObject();
                }
            });
            get("/guild/:id/channels", (req, res) -> {
                final List<Channel> channels = getCache().getGuildChannels(req.params(":id"));
                if(channels != null && !channels.isEmpty()) {
                    return new JSONArray(channels);
                } else {
                    res.status(404);
                    return new JSONArray();
                }
            });
            get("/guild/:id/roles", (req, res) -> {
                final List<Role> roles = getCache().getGuildRoles(req.params(":id"));
                if(roles != null && !roles.isEmpty()) {
                    return new JSONArray(roles);
                } else {
                    res.status(404);
                    return new JSONArray();
                }
            });
            get("/channel/:id", (req, res) -> {
                final Channel channel = getCache().getChannel(req.params(":id"));
                if(channel != null) {
                    return new JSONObject(channel);
                } else {
                    res.status(404);
                    return new JSONObject();
                }
            });
            get("/role/:id", (req, res) -> {
                final Role role = getCache().getRole(req.params(":id"));
                if(role != null) {
                    return new JSONObject(role);
                } else {
                    return new JSONObject();
                }
            });
        });
        path("/data", () -> {
            
            //noinspection CodeBlock2Expr
            path("/guild", () -> {
                //noinspection CodeBlock2Expr
                path("/:id", () -> {
                    path("/config", () -> {
                        get("/:type", (req, res) -> {
                            final PluginSettings settings = getDatabase().getOrBaseSettings(req.params(":type"), req.params(":id"));
                            return MAPPER.writeValueAsString(settings);
                        });
                        post("/:type", (req, res) -> {
                            final JSONObject data = new JSONObject(req.body());
                            final PluginSettings settings = getDatabase().getOrBaseSettings(req.params(":type"), req.params(":id"));
                            if(settings.validate(data)) {
                                try {
                                    if(settings.updateSettings(getDatabase(), data)) {
                                        // All good, update and return
                                        logger.info("Updated {} settings for {}", req.params(":type"), req.params(":id"));
                                        return new JSONObject().put("status", "ok");
                                    } else {
                                        logger.info("{} settings for {} failed updateSettings", req.params(":type"), req.params(":id"));
                                        return new JSONObject().put("status", "error").put("error", "invalid config");
                                    }
                                } catch(final RuntimeException e) {
                                    logger.error("{} settings for {} failed updateSettings expectedly", req.params(":type"), req.params(":id"));
                                    e.printStackTrace();
                                    return new JSONObject().put("status", "error").put("error", "invalid config");
                                } catch(final Exception e) {
                                    logger.error("{} settings for {} failed updateSettings unexpectedly", req.params(":type"), req.params(":id"));
                                    logger.error("Caught unknown exception updating:");
                                    e.printStackTrace();
                                    return new JSONObject().put("status", "error").put("error", "very invalid config");
                                }
                            } else {
                                logger.error("{} settings for {} failed validate", req.params(":type"), req.params(":id"));
                                // :fire: :blobcatfireeyes:, send back an error
                                return new JSONObject().put("status", "error").put("error", "invalid config");
                            }
                        });
                    });
                });
            });
            
            path("backgrounds", () -> {
                // More shit goes here
                
            });
            
            path("/commands", () -> {
                // More shit goes here
                get("/metadata", (req, res) -> new JSONArray(commandManager.getCommandMetadata()));
            });
            
            path("/plugins", () -> {
                // More shit goes here
                //noinspection TypeMayBeWeakened
                final JSONArray data = new JSONArray(pluginManager.getPluginMetadata());
                data.forEach(e -> {
                    // ;-;
                    // TODO: Find better solution
                    ((JSONObject) e).remove("settingsClass");
                    ((JSONObject) e).remove("pluginClass");
                });
                get("/metadata", (req, res) -> data);
            });
            get("/player/:id", (req, res) -> new JSONObject(database.getPlayer(req.params(":id"))));
        });
    }
    
    @SuppressWarnings("WeakerAccess")
    public DiscordCache getCache() {
        return eventManager.getCache();
    }
}
