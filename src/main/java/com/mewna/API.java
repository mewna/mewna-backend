package com.mewna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.mewna.accounts.Account;
import com.mewna.catnip.entity.user.User;
import com.mewna.data.DiscordCache;
import com.mewna.data.Player;
import com.mewna.data.PluginSettings;
import com.mewna.data.Webhook;
import com.mewna.plugin.plugins.PluginLevels;
import com.mewna.plugin.util.TextureManager;
import io.sentry.Sentry;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        
        final HttpServer server = mewna.vertx().createHttpServer();
        
        final Router router = Router.router(mewna.vertx());
        
        // Cache
        {
            router.get("/cache/user/:id").handler(ctx -> {
                final String id = ctx.request().getParam("id");
                DiscordCache.user(id).thenAccept(user -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(user.toJson().encode());
                }).exceptionally(e -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .setStatusCode(404)
                            .end(new JsonObject().encode());
                    return null;
                });
            });
            router.get("/cache/guild/:id").handler(ctx -> {
                final String id = ctx.request().getParam("id");
                DiscordCache.guild(id).thenAccept(guild -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(guild.toJson().encode());
                }).exceptionally(e -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .setStatusCode(404)
                            .end(new JsonObject().encode());
                    return null;
                });
            });
            router.get("/cache/guild/:id/channels").handler(ctx -> {
                final String id = ctx.request().getParam("id");
                DiscordCache.channels(id).thenAccept(channels -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonArray(new ArrayList<>(channels)).encode());
                }).exceptionally(e -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .setStatusCode(404)
                            .end(new JsonArray().encode());
                    return null;
                });
            });
            router.get("/cache/guild/:id/roles").handler(ctx -> {
                final String id = ctx.request().getParam("id");
                DiscordCache.roles(id).thenAccept(roles -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonArray(new ArrayList<>(roles)).encode());
                }).exceptionally(e -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .setStatusCode(404)
                            .end(new JsonArray().encode());
                    return null;
                });
            });
        }
        
        // Data
        {
            // Accounts
            router.get("/data/account/:id").blockingHandler(ctx -> {
                final String id = ctx.request().getParam("id");
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(JsonObject.mapFrom(mewna.database().getAccountById(id)).encode());
            });
            router.get("/data/account/:id/links").blockingHandler(ctx -> {
                final String id = ctx.request().getParam("id");
                ctx.response().putHeader("Content-Type", "application/json");
                final Optional<Account> maybeAccount = mewna.accountManager().getAccountById(id);
                if(maybeAccount.isPresent()) {
                    final Account account = maybeAccount.get();
                    final JsonObject data = new JsonObject();
                    data.put("discord", account.discordAccountId());
                    ctx.response().end(data.encode());
                } else {
                    ctx.response().end(new JsonObject().put("error", "no links").encode());
                }
            });
            router.get("/data/account/:id/profile").blockingHandler(ctx -> {
                final String id = ctx.request().getParam("id");
                ctx.response().putHeader("Content-Type", "application/json");
                final Optional<Account> maybeAccount = mewna.accountManager().getAccountById(id);
                if(maybeAccount.isPresent()) {
                    final Account account = maybeAccount.get();
                    final JsonObject data = new JsonObject();
                    data.put("id", account.id())
                            .put("username", account.username())
                            .put("displayName", account.discordAccountId())
                            .put("avatar", account.avatar())
                            .put("aboutText", account.aboutText())
                            .put("customBackground", account.customBackground())
                            .put("ownedBackgroundPacks", account.ownedBackgroundPacks())
                            .put("isInBeta", account.isInBeta())
                    ;
                    ctx.response().end(data.encode());
                } else {
                    ctx.response().end(new JsonObject().put("error", "no account").encode());
                }
            });
            router.get("/data/account/:id/posts").blockingHandler(ctx -> {
                final String id = ctx.request().getParam("id");
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(new JsonArray(mewna.database().getLast100TimelinePosts(id)
                                .stream()
                                .map(JsonObject::mapFrom)
                                .collect(Collectors.toList()))
                                .encode());
            });
            router.get("/data/account/:id/posts/all").blockingHandler(ctx -> {
                final String id = ctx.request().getParam("id");
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(new JsonArray(mewna.database().getAllTimelinePosts(id)
                                .stream()
                                .map(JsonObject::mapFrom)
                                .collect(Collectors.toList()))
                                .encode());
            });
            router.post("/data/account/update").blockingHandler(ctx -> {
                final JsonObject body = ctx.getBodyAsJson();
                mewna.accountManager().updateAccountSettings(body);
                ctx.response().putHeader("Content-Type", "application/json").end(new JsonObject().encode());
            });
            router.post("/data/account/update/oauth").blockingHandler(ctx -> {
                final JsonObject body = ctx.getBodyAsJson();
                mewna.accountManager().createOrUpdateDiscordOAuthLinkedAccount(body);
                ctx.response().putHeader("Content-Type", "application/json").end(new JsonObject().encode());
            });
            router.get("/data/account/links/discord/:id").blockingHandler(ctx -> {
                ctx.response().end(mewna.accountManager().checkDiscordLinkedAccountExists(ctx.request().getParam("id")));
            });
            // Guilds
            router.get("/data/guild/:id/config/:type").blockingHandler(ctx -> {
                final String id = ctx.request().getParam("id");
                final String type = ctx.request().getParam("type");
                final PluginSettings settings = mewna.database().getOrBaseSettings(type, id);
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(JsonObject.mapFrom(settings).encode());
            });
            router.post("/data/guild/:id/config/:type").blockingHandler(ctx -> {
                final String type = ctx.request().getParam("type");
                final String id = ctx.request().getParam("id");
                final JsonObject data = ctx.getBodyAsJson();
                final PluginSettings settings = mewna.database().getOrBaseSettings(type, id);
                ctx.response().putHeader("Content-Type", "application/json");
                if(settings.validate(data)) {
                    try {
                        if(settings.updateSettings(mewna.database(), data)) {
                            // All good, update and return
                            logger.info("Updated {} settings for {}", type, id);
                            ctx.response().end(new JsonObject().put("status", "ok").encode());
                        } else {
                            logger.info("{} settings for {} failed updateSettings", type, id);
                            ctx.response().end(new JsonObject().put("status", "error").put("error", "invalid config").encode());
                        }
                    } catch(final RuntimeException e) {
                        logger.error("{} settings for {} failed updateSettings expectedly", type, id);
                        e.printStackTrace();
                        Sentry.capture(e);
                        ctx.response().end(new JsonObject().put("status", "error").put("error", "invalid config").encode());
                    } catch(final Exception e) {
                        logger.error("{} settings for {} failed updateSettings unexpectedly", type, id);
                        logger.error("Caught unknown exception updating:");
                        e.printStackTrace();
                        Sentry.capture(e);
                        ctx.response().end(new JsonObject().put("status", "error").put("error", "very invalid config").encode());
                    }
                } else {
                    logger.error("{} settings for {} failed validate", type, id);
                    // :fire: :blobcatfireeyes:, send back an error
                    ctx.response().end(new JsonObject().put("status", "error").put("error", "invalid config").encode());
                }
            });
            router.get("/data/guild/:id/levels").blockingHandler(ctx -> {
                final String id = ctx.request().getParam("id");
                // This makes me feel better about not using a prepared query
                if(!id.matches("\\d{17,20}")) {
                    ctx.response().end(new JsonObject().encode());
                    return;
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
                mewna.database().getStore().sql(query, p -> {
                    //noinspection Convert2MethodRef
                    final ResultSet resultSet = p.executeQuery();
                    if(resultSet.isBeforeFirst()) {
                        int counter = 1;
                        while(resultSet.next()) {
                            try {
                                final Player player = MAPPER.readValue(resultSet.getString("player"), Player.class);
                                final Account account = MAPPER.readValue(resultSet.getString("account"), Account.class);
                                
                                final User user = DiscordCache.user(player.getId()).toCompletableFuture().join();
                                final long userXp = player.getXp(id);
                                final long userLevel = PluginLevels.xpToLevel(userXp);
                                final long nextLevel = userLevel + 1;
                                final long currentLevelXp = PluginLevels.fullLevelToXp(userLevel);
                                final long nextLevelXp = PluginLevels.fullLevelToXp(nextLevel);
                                final long xpNeeded = PluginLevels.nextLevelXp(userXp);
                                
                                results.add(new JsonObject()
                                        .put("name", user.username())
                                        .put("discrim", user.discriminator())
                                        .put("avatar", user.effectiveAvatarUrl())
                                        .put("userXp", userXp)
                                        .put("userLevel", userLevel)
                                        .put("nextLevel", nextLevel)
                                        .put("playerRank", (long) counter)
                                        .put("currentLevelXp", currentLevelXp)
                                        .put("xpNeeded", xpNeeded)
                                        .put("nextLevelXp", nextLevelXp)
                                        .put("customBackground", account.customBackground())
                                        .put("accountId", account.id())
                                );
                            } catch(final IOException e) {
                                Sentry.capture(e);
                                e.printStackTrace();
                            }
                            ++counter;
                        }
                    }
                });
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(new JsonArray(results).encode());
            });
            router.post("/data/guild/:id/webhooks").blockingHandler(ctx -> {
                final JsonArray arr = new JsonArray(
                        mewna.database()
                                .getAllWebhooks(ctx.request().getParam("id"))
                                .stream()
                                .map(e -> {
                                    final JsonObject j = JsonObject.mapFrom(e);
                                    j.remove("secret");
                                    return j;
                                })
                                .collect(Collectors.toList())
                );
                
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(arr.encode());
            });
            router.get("/data/guild/:id/webhooks/:channel_id").blockingHandler(ctx -> {
                final String channel = ctx.request().getParam("channel_id");
                final Optional<Webhook> webhook = mewna.database().getWebhook(channel);
                final JsonObject hook = webhook.map(JsonObject::mapFrom).orElseGet(JsonObject::new);
                if(hook.containsKey("secret")) {
                    hook.remove("secret");
                }
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(hook.encode());
            });
            router.post("/data/guild/:id/webhooks/add").blockingHandler(ctx -> {
                final Webhook hook = Webhook.fromJson(ctx.getBodyAsJson());
                mewna.database().addWebhook(hook);
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(new JsonObject().encode());
            });
            // Backgrounds
            router.get("/data/backgrounds/packs").handler(ctx -> {
                ctx.response().end(JsonObject.mapFrom(TextureManager.getPacks()).encode());
            });
            // Store
            router.post("/data/store/checkout/start").blockingHandler(ctx -> {
                final JsonObject body = ctx.getBodyAsJson();
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(mewna.paypalHandler().startPayment(
                                body.getString("userId"),
                                body.getString("sku")
                        ).encode());
            });
            router.post("/data/store/checkout/confirm").blockingHandler(ctx -> {
                final JsonObject body = ctx.getBodyAsJson();
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(mewna.paypalHandler().finishPayment(
                                body.getString("userId"),
                                body.getString("paymentId"),
                                body.getString("payerId")
                        ).encode());
            });
            router.get("/data/store/manifest").handler(ctx -> {
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(new JsonArray(ImmutableList.copyOf(mewna.paypalHandler().getSkus())).encode());
            });
            // Commands
            router.get("/data/commands/metadata").handler(ctx -> {
                final JsonArray arr = new JsonArray(mewna.commandManager().getCommandMetadata()
                        .stream()
                        .map(JsonObject::mapFrom)
                        .collect(Collectors.toList()));
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(arr.encode());
            });
            // Plugins
            router.get("/data/plugins/metadata").handler(ctx -> {
                final JsonArray data = new JsonArray(mewna.pluginManager().getPluginMetadata().stream()
                        .map(JsonObject::mapFrom)
                        .collect(Collectors.toList()));
                data.forEach(e -> {
                    // ;-;
                    // TODO: Find better solution
                    ((JsonObject) e).remove("settingsClass");
                    ((JsonObject) e).remove("pluginClass");
                });
                ctx.response().putHeader("Content-Type", "application/json").end(data.encode());
            });
            // Player
            router.get("/data/player/:id").blockingHandler(ctx -> {
                final String id = ctx.request().getParam("id");
                ctx.response().putHeader("Content-Type", "application/json")
                        .end(mewna.database().getOptionalPlayer(id)
                                .map(JsonObject::mapFrom)
                                .orElse(new JsonObject())
                                .encode());
            });
        }
    
        server.requestHandler(router::accept)
                .listen(Integer.parseInt(Optional.ofNullable(System.getenv("PORT"))
                        .orElse("80")));
    }
}
