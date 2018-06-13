package com.mewna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.Role;
import com.mewna.cache.entity.User;
import com.mewna.data.Player;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginLevels;
import com.mewna.plugin.util.TextureManager;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static spark.Spark.*;

/**
 * @author amy
 * @since 6/10/18.
 */
@RequiredArgsConstructor
class API {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Mewna mewna;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    void start() {
        logger.info("Starting API server...");
        port(Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("80")));
        path("/cache", () -> {
            get("/user/:id", (req, res) -> {
                final User user = mewna.getCache().getUser(req.params(":id"));
                if(user != null) {
                    return new JSONObject(user);
                } else {
                    res.status(404);
                    return new JSONObject();
                }
            });
            get("/guild/:id", (req, res) -> {
                final Guild guild = mewna.getCache().getGuild(req.params(":id"));
                if(guild != null) {
                    return new JSONObject(guild);
                } else {
                    res.status(404);
                    return new JSONObject();
                }
            });
            get("/guild/:id/channels", (req, res) -> {
                final List<Channel> channels = mewna.getCache().getGuildChannels(req.params(":id"));
                if(channels != null && !channels.isEmpty()) {
                    return new JSONArray(channels);
                } else {
                    res.status(404);
                    return new JSONArray();
                }
            });
            get("/guild/:id/roles", (req, res) -> {
                final List<Role> roles = mewna.getCache().getGuildRoles(req.params(":id"));
                if(roles != null && !roles.isEmpty()) {
                    return new JSONArray(roles);
                } else {
                    res.status(404);
                    return new JSONArray();
                }
            });
            get("/channel/:id", (req, res) -> {
                final Channel channel = mewna.getCache().getChannel(req.params(":id"));
                if(channel != null) {
                    return new JSONObject(channel);
                } else {
                    res.status(404);
                    return new JSONObject();
                }
            });
            get("/role/:id", (req, res) -> {
                final Role role = mewna.getCache().getRole(req.params(":id"));
                if(role != null) {
                    return new JSONObject(role);
                } else {
                    return new JSONObject();
                }
            });
        });
        path("/data", () -> {
            path("/player", () -> {
                // More shit goes here
                get("/:id", (req, res) -> new JSONObject(mewna.getDatabase().getPlayer(req.params(":id"))));
                post("/:id", (req, res) -> {
                    final JSONObject data = new JSONObject(req.body());
                    final Player player = mewna.getDatabase().getPlayer(req.params(":id"));
                    if(player.validateSettings(data)) {
                        try {
                            player.updateSettings(mewna.getDatabase(), data);
                            logger.info("Updated player {} settings", req.params(":id"));
                            // All good, update and return
                            
                            return new JSONObject().put("status", "ok");
                        } catch(final RuntimeException e) {
                            logger.error("Player settings for {} failed updateSettings expectedly", req.params(":id"));
                            e.printStackTrace();
                            return new JSONObject().put("status", "error").put("error", "invalid config");
                        } catch(final Exception e) {
                            logger.error("Player settings for {} failed updateSettings unexpectedly", req.params(":id"));
                            logger.error("Caught unknown exception updating:");
                            e.printStackTrace();
                            return new JSONObject().put("status", "error").put("error", "very invalid config");
                        }
                    } else {
                        logger.error("Player settings for {} failed validate", req.params(":id"));
                        // :fire: :blobcatfireeyes:, send back an error
                        return new JSONObject().put("status", "error").put("error", "invalid config");
                    }
                });
            });
            //noinspection CodeBlock2Expr
            path("/guild", () -> {
                //noinspection CodeBlock2Expr
                path("/:id", () -> {
                    path("/config", () -> {
                        get("/:type", (req, res) -> {
                            final PluginSettings settings = mewna.getDatabase().getOrBaseSettings(req.params(":type"), req.params(":id"));
                            return MAPPER.writeValueAsString(settings);
                        });
                        post("/:type", (req, res) -> {
                            final JSONObject data = new JSONObject(req.body());
                            final PluginSettings settings = mewna.getDatabase().getOrBaseSettings(req.params(":type"), req.params(":id"));
                            if(settings.validate(data)) {
                                try {
                                    if(settings.updateSettings(mewna.getDatabase(), data)) {
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
                    get("/levels", (req, res) -> {
                        final String id = req.params(":id");
                        if(!id.matches("\\d{17,20}")) {
                            return new JSONObject();
                        }
                        // This is gonna be ugly
                        final List<JSONObject> results = new ArrayList<>();
                        final String query = String.format("SELECT data FROM players WHERE data->'guildXp'->'%s' IS NOT NULL " +
                                "AND (data->'guildXp'->>'%s')::integer > 0 " +
                                "ORDER BY data->'guildXp'->'%s' DESC LIMIT 100;", id, id, id);
                        mewna.getDatabase().getStore().sql(query, p -> {
                            //noinspection Convert2MethodRef
                            final ResultSet resultSet = p.executeQuery();
                            if(resultSet.isBeforeFirst()) {
                                int counter = 1;
                                while(resultSet.next()) {
                                    final JSONObject data = new JSONObject(resultSet.getString("data"));
                                    final User user = mewna.getCache().getUser(data.getString("id"));
                                    final long userXp = data.getJSONObject("guildXp").getLong(id);
                                    final long userLevel = PluginLevels.xpToLevel(userXp);
                                    final long nextLevel = userLevel + 1;
                                    final long playerRank = counter;
                                    final long currentLevelXp = PluginLevels.fullLevelToXp(userLevel);
                                    final long nextLevelXp = PluginLevels.fullLevelToXp(nextLevel);
                                    final long xpNeeded = PluginLevels.nextLevelXp(userXp);
                                    results.add(new JSONObject()
                                            .put("name", user.getName())
                                            .put("discrim", user.getDiscriminator())
                                            .put("avatar", user.getAvatarURL())
                                            .put("userXp", userXp)
                                            .put("userLevel", userLevel)
                                            .put("nextLevel", nextLevel)
                                            .put("playerRank", playerRank)
                                            .put("currentLevelXp", currentLevelXp)
                                            .put("xpNeeded", xpNeeded)
                                            .put("nextLevelXp", nextLevelXp)
                                    );
                                    ++counter;
                                }
                            }
                        });
                        return new JSONArray(results);
                    });
                });
            });
            
            path("/backgrounds", () -> {
                // More shit goes here
                get("/packs", (req, res) -> new JSONObject(TextureManager.getPacks()));
            });
            
            path("/commands", () -> {
                // More shit goes here
                get("/metadata", (req, res) -> new JSONArray(mewna.getCommandManager().getCommandMetadata()));
            });
            
            path("/plugins", () -> {
                // More shit goes here
                //noinspection TypeMayBeWeakened
                final JSONArray data = new JSONArray(mewna.getPluginManager().getPluginMetadata());
                data.forEach(e -> {
                    // ;-;
                    // TODO: Find better solution
                    ((JSONObject) e).remove("settingsClass");
                    ((JSONObject) e).remove("pluginClass");
                });
                get("/metadata", (req, res) -> data);
            });
            get("/player/:id", (req, res) -> new JSONObject(mewna.getDatabase().getPlayer(req.params(":id"))));
        });
    }
}
