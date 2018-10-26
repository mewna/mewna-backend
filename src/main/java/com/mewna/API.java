package com.mewna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.mewna.accounts.Account;
import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.Role;
import com.mewna.cache.entity.User;
import com.mewna.data.Player;
import com.mewna.data.PluginSettings;
import com.mewna.data.Webhook;
import com.mewna.plugin.plugins.PluginLevels;
import com.mewna.plugin.util.TextureManager;
import io.sentry.Sentry;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    
    @SuppressWarnings({"CodeBlock2Expr", "UnnecessarilyQualifiedInnerClassAccess"})
    void start() {
        logger.info("Starting API server...");
        port(Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("80")));
        path("/cache", () -> {
            get("/user/:id", (req, res) -> {
                final User user = mewna.getCache().getUser(req.params(":id"));
                if(user != null) {
                    return JsonObject.mapFrom(user);
                } else {
                    res.status(404);
                    return new JsonObject();
                }
            });
            get("/guild/:id", (req, res) -> {
                final Guild guild = mewna.getCache().getGuild(req.params(":id"));
                if(guild != null && guild.getId() != null) {
                    return JsonObject.mapFrom(guild);
                } else {
                    res.status(404);
                    return new JsonObject();
                }
            });
            get("/guild/:id/channels", (req, res) -> {
                final List<Channel> channels = mewna.getCache().getGuildChannels(req.params(":id"));
                if(channels != null && !channels.isEmpty()) {
                    return new JsonArray(channels);
                } else {
                    res.status(404);
                    return new JsonArray();
                }
            });
            get("/guild/:id/roles", (req, res) -> {
                final List<Role> roles = mewna.getCache().getGuildRoles(req.params(":id"));
                if(roles != null && !roles.isEmpty()) {
                    return new JsonArray(roles);
                } else {
                    res.status(404);
                    return new JsonArray();
                }
            });
            get("/channel/:id", (req, res) -> {
                final Channel channel = mewna.getCache().getChannel(req.params(":id"));
                if(channel != null) {
                    return JsonObject.mapFrom(channel);
                } else {
                    res.status(404);
                    return new JsonObject();
                }
            });
            get("/role/:id", (req, res) -> {
                final Role role = mewna.getCache().getRole(req.params(":id"));
                if(role != null) {
                    return JsonObject.mapFrom(role);
                } else {
                    return new JsonObject();
                }
            });
        });
        path("/data", () -> {
            path("/account", () -> {
                path("/:id", () -> {
                    get("/", (req, res) -> {
                        return JsonObject.mapFrom(mewna.getDatabase().getAccountById(req.params(":id")));
                    });
                    get("/links", (req, res) -> {
                        final Optional<Account> maybeAccount = mewna.getAccountManager().getAccountById(req.params(":id"));
                        if(maybeAccount.isPresent()) {
                            final Account account = maybeAccount.get();
                            final JsonObject data = new JsonObject();
                            data
                                    .put("discord", account.getDiscordAccountId())
                            ;
                            return data;
                        } else {
                            return new JsonObject().put("error", "no links");
                        }
                    });
                    get("/profile", (req, res) -> {
                        final Optional<Account> maybeAccount = mewna.getAccountManager().getAccountById(req.params(":id"));
                        if(maybeAccount.isPresent()) {
                            final Account account = maybeAccount.get();
                            final JsonObject data = new JsonObject();
                            data.put("id", account.getId())
                                    .put("username", account.getUsername())
                                    .put("displayName", account.getDisplayName())
                                    .put("avatar", account.getAvatar())
                                    .put("aboutText", account.getAboutText())
                                    .put("customBackground", account.getCustomBackground())
                                    .put("ownedBackgroundPacks", account.getOwnedBackgroundPacks())
                                    .put("isInBeta", account.isInBeta())
                            ;
                            return data;
                        } else {
                            return new JsonObject().put("error", "no account");
                        }
                    });
                    get("/posts/all", (req, res) -> {
                        return new JsonArray(mewna.getDatabase().getAllTimelinePosts(req.params(":id")));
                    });
                    get("/posts", (req, res) -> {
                        return new JsonArray(mewna.getDatabase().getLast100TimelinePosts(req.params(":id")));
                    });
                });
                post("/update", (req, res) -> {
                    mewna.getAccountManager().updateAccountSettings(JsonObject.mapFrom(req.body()));
                    return new JsonObject();
                });
                post("/update/oauth", (req, res) -> {
                    mewna.getAccountManager().createOrUpdateDiscordOAuthLinkedAccount(JsonObject.mapFrom(req.body()));
                    return new JsonObject();
                });
                
                path("/links", () -> {
                    path("/discord", () -> {
                        get("/:id", (req, res) -> {
                            return mewna.getAccountManager().checkDiscordLinkedAccountExists(req.params(":id"));
                        });
                    });
                });
            });
            path("/guild", () -> {
                path("/:id", () -> {
                    path("/config", () -> {
                        get("/:type", (req, res) -> {
                            final PluginSettings settings = mewna.getDatabase().getOrBaseSettings(req.params(":type"), req.params(":id"));
                            return MAPPER.writeValueAsString(settings);
                        });
                        post("/:type", (req, res) -> {
                            final JsonObject data = JsonObject.mapFrom(req.body());
                            final PluginSettings settings = mewna.getDatabase().getOrBaseSettings(req.params(":type"), req.params(":id"));
                            if(settings.validate(data)) {
                                try {
                                    if(settings.updateSettings(mewna.getDatabase(), data)) {
                                        // All good, update and return
                                        logger.info("Updated {} settings for {}", req.params(":type"), req.params(":id"));
                                        return new JsonObject().put("status", "ok");
                                    } else {
                                        logger.info("{} settings for {} failed updateSettings", req.params(":type"), req.params(":id"));
                                        return new JsonObject().put("status", "error").put("error", "invalid config");
                                    }
                                } catch(final RuntimeException e) {
                                    logger.error("{} settings for {} failed updateSettings expectedly", req.params(":type"), req.params(":id"));
                                    e.printStackTrace();
                                    Sentry.capture(e);
                                    return new JsonObject().put("status", "error").put("error", "invalid config");
                                } catch(final Exception e) {
                                    logger.error("{} settings for {} failed updateSettings unexpectedly", req.params(":type"), req.params(":id"));
                                    logger.error("Caught unknown exception updating:");
                                    e.printStackTrace();
                                    Sentry.capture(e);
                                    return new JsonObject().put("status", "error").put("error", "very invalid config");
                                }
                            } else {
                                logger.error("{} settings for {} failed validate", req.params(":type"), req.params(":id"));
                                // :fire: :blobcatfireeyes:, send back an error
                                return new JsonObject().put("status", "error").put("error", "invalid config");
                            }
                        });
                    });
                    get("/levels", (req, res) -> {
                        final String id = req.params(":id");
                        // This makes me feel better about not using a prepared query
                        if(!id.matches("\\d{17,20}")) {
                            return new JsonObject();
                        }
                        // This is gonna be ugly
                        final List<JsonObject> results = new ArrayList<>();
                        final String query = String.format(
                                "SELECT players.data AS player, accounts.data AS account FROM players\n" +
                                        "    JOIN accounts ON accounts.data->>'discordAccountId' = players.id\n" +
                                        "    WHERE players.data->'guildXp' ?? '%s'\n" +
                                        "        AND (players.data->'guildXp'->>'%s')::integer > 0\n" +
                                        "    ORDER BY (players.data->'guildXp'->>'%s')::integer DESC LIMIT 100;",
                                id, id, id
                        );
                        mewna.getDatabase().getStore().sql(query, p -> {
                            //noinspection Convert2MethodRef
                            final ResultSet resultSet = p.executeQuery();
                            if(resultSet.isBeforeFirst()) {
                                int counter = 1;
                                while(resultSet.next()) {
                                    try {
                                        final Player player = MAPPER.readValue(resultSet.getString("player"), Player.class);
                                        final Account account = MAPPER.readValue(resultSet.getString("account"), Account.class);
                                        
                                        final User user = mewna.getCache().getUser(player.getId());
                                        final long userXp = player.getXp(id);
                                        final long userLevel = PluginLevels.xpToLevel(userXp);
                                        final long nextLevel = userLevel + 1;
                                        final long currentLevelXp = PluginLevels.fullLevelToXp(userLevel);
                                        final long nextLevelXp = PluginLevels.fullLevelToXp(nextLevel);
                                        final long xpNeeded = PluginLevels.nextLevelXp(userXp);
                                        
                                        results.add(new JsonObject()
                                                .put("name", user.getName())
                                                .put("discrim", user.getDiscriminator())
                                                .put("avatar", user.getAvatarURL())
                                                .put("userXp", userXp)
                                                .put("userLevel", userLevel)
                                                .put("nextLevel", nextLevel)
                                                .put("playerRank", (long) counter)
                                                .put("currentLevelXp", currentLevelXp)
                                                .put("xpNeeded", xpNeeded)
                                                .put("nextLevelXp", nextLevelXp)
                                                .put("customBackground", account.getCustomBackground())
                                                .put("accountId", account.getId())
                                        );
                                    } catch(final IOException e) {
                                        Sentry.capture(e);
                                        e.printStackTrace();
                                    }
                                    ++counter;
                                }
                            }
                        });
                        return new JsonArray(results);
                    });
                    get("/webhooks", (req, res) -> new JsonArray(mewna.getDatabase().getAllWebhooks(req.params(":id")).stream().map(e -> {
                        final JsonObject j = JsonObject.mapFrom(e);
                        j.remove("secret");
                        return j;
                    }).collect(Collectors.toList())));
                    get("/webhooks/:channel_id", (req, res) -> {
                        final String channel = req.params(":channel_id");
                        final Optional<Webhook> webhook = mewna.getDatabase().getWebhook(channel);
                        final JsonObject hook = webhook.map(JsonObject::mapFrom).orElseGet(JsonObject::new);
                        if(hook.containsKey("secret")) {
                            hook.remove("secret");
                        }
                        return hook;
                    });
                    post("/webhooks/add", (req, res) -> {
                        final Webhook hook = Webhook.fromJson(JsonObject.mapFrom(req.body()));
                        mewna.getDatabase().addWebhook(hook);
                        return new JsonObject();
                    });
                });
            });
            
            path("/backgrounds", () -> {
                // More shit goes here
                get("/packs", (req, res) -> JsonObject.mapFrom(TextureManager.getPacks()));
            });
            
            path("/store", () -> {
                path("/checkout", () -> {
                    post("/start", (req, res) -> {
                        final JsonObject body = JsonObject.mapFrom(req.body());
                        return mewna.getPaypalHandler().startPayment(
                                body.getString("userId"),
                                body.getString("sku")
                        );
                    });
                    post("/confirm", (req, res) -> {
                        final JsonObject body = JsonObject.mapFrom(req.body());
                        return mewna.getPaypalHandler().finishPayment(
                                body.getString("userId"),
                                body.getString("paymentId"),
                                body.getString("payerId")
                        );
                    });
                });
                get("/manifest", (req, res) -> new JsonArray(ImmutableList.copyOf(mewna.getPaypalHandler().getSkus())));
            });
            
            path("/commands", () -> {
                // More shit goes here
                get("/metadata", (req, res) -> new JsonArray(ImmutableList.copyOf(mewna.getCommandManager().getCommandMetadata())));
            });
            
            path("/plugins", () -> {
                // More shit goes here
                //noinspection TypeMayBeWeakened
                get("/metadata", (req, res) -> {
                    @SuppressWarnings("TypeMayBeWeakened")
                    final JsonArray data = new JsonArray(ImmutableList.copyOf(mewna.getPluginManager().getPluginMetadata()));
                    data.forEach(e -> {
                        // ;-;
                        // TODO: Find better solution
                        ((JsonObject) e).remove("settingsClass");
                        ((JsonObject) e).remove("pluginClass");
                    });
                    return data;
                });
            });
            get("/player/:id", (req, res) -> JsonObject.mapFrom(mewna.getDatabase().getPlayer(req.params(":id"))));
        });
    }
}
