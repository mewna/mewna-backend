package com.mewna;

import com.google.common.collect.ImmutableList;
import com.mewna.accounts.Account;
import com.mewna.catnip.entity.channel.Channel;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.catnip.entity.user.User;
import com.mewna.data.DiscordCache;
import com.mewna.data.Player;
import com.mewna.data.Webhook;
import com.mewna.plugin.plugins.PluginEconomy;
import com.mewna.plugin.plugins.PluginLevels;
import com.mewna.plugin.plugins.levels.LevelsImporter;
import com.mewna.plugin.util.TextureManager;
import com.mewna.servers.ServerBlogPost;
import io.sentry.Sentry;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mewna.util.Async.move;
import static com.mewna.util.Translator.$;

/**
 * @author amy
 * @since 6/10/18.
 */
@RequiredArgsConstructor
class API {
    private final Mewna mewna;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private List<JsonObject> minifyChannels(final Collection<Channel> channels) {
        return channels.stream()
                .map(e -> new JsonObject()
                        .put("id", e.id())
                        .put("type", e.type().getKey())
                        .put("guildId", e.asGuildChannel().guildId())
                        .put("name", e.asGuildChannel().name())
                        .put("position", e.asGuildChannel().position())
                        .put("nsfw", e.isText() && e.asTextChannel().nsfw())
                )
                .collect(Collectors.toList());
    }
    
    private List<JsonObject> minifyRoles(final Collection<Role> channels) {
        return channels.stream()
                .map(e -> new JsonObject()
                        .put("id", e.id())
                        .put("guildId", e.guildId())
                        .put("name", e.name())
                        .put("position", e.position())
                        .put("color", e.color())
                        .put("managed", e.managed())
                )
                .collect(Collectors.toList());
    }
    
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
                            .end(user.toJson().put("id", user.id()).encode());
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
                            .end(guild.toJson().put("id", guild.id()).encode());
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
                            .end(new JsonArray(minifyChannels(channels)).encode());
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
                            .end(new JsonArray(minifyRoles(roles)).encode());
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
            router.get("/data/account/:id").handler(ctx -> {
                move(() -> {
                    final String id = ctx.request().getParam("id");
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(JsonObject.mapFrom(mewna.database().getAccountById(id)).encode());
                });
            });
            router.get("/data/account/:id/links").handler(ctx -> {
                move(() -> {
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
            });
            router.get("/data/account/:id/profile").handler(ctx -> {
                move(() -> {
                    final String id = ctx.request().getParam("id");
                    ctx.response().putHeader("Content-Type", "application/json");
                    final Optional<Account> maybeAccount = mewna.accountManager().getAccountById(id);
                    if(maybeAccount.isPresent()) {
                        final Account account = maybeAccount.get();
                        final JsonObject data = new JsonObject();
                        data.put("id", account.id())
                                .put("username", account.username())
                                .put("displayName", account.displayName())
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
            });
            router.get("/data/account/:id/posts").handler(ctx -> {
                move(() -> {
                    final String id = ctx.request().getParam("id");
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonArray(mewna.database().getLast100TimelinePosts(id)
                                    .stream()
                                    .map(JsonObject::mapFrom)
                                    .collect(Collectors.toList()))
                                    .encode());
                });
            });
            router.get("/data/account/:id/posts/all").handler(ctx -> {
                move(() -> {
                    final String id = ctx.request().getParam("id");
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonArray(mewna.database().getAllTimelinePosts(id)
                                    .stream()
                                    .map(JsonObject::mapFrom)
                                    .collect(Collectors.toList()))
                                    .encode());
                });
            });
            router.post("/data/account/update").handler(BodyHandler.create()).handler(ctx -> {
                move(() -> {
                    final JsonObject body = ctx.getBodyAsJson();
                    mewna.accountManager().updateAccountSettings(body);
                    ctx.response().putHeader("Content-Type", "application/json").end(new JsonObject().encode());
                });
            });
            router.post("/data/account/update/oauth").handler(BodyHandler.create()).handler(ctx -> {
                move(() -> {
                    final JsonObject body = ctx.getBodyAsJson();
                    mewna.accountManager().createOrUpdateDiscordOAuthLinkedAccount(body);
                    ctx.response().putHeader("Content-Type", "application/json").end(new JsonObject().encode());
                });
            });
            router.get("/data/account/links/discord/:id").handler(ctx -> {
                move(() -> {
                    ctx.response().end(mewna.accountManager().checkDiscordLinkedAccountExists(ctx.request().getParam("id")));
                });
            });
            
            // Guilds
            
            router.get("/data/guild/:id/config/:type").handler(ctx -> {
                final String id = ctx.request().getParam("id");
                final String type = ctx.request().getParam("type");
                mewna.database().getOrBaseSettings(type, id).thenAccept(settings -> {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(JsonObject.mapFrom(settings).encode());
                });
            });
            router.post("/data/guild/:id/config/:type").handler(BodyHandler.create()).handler(ctx -> {
                final String type = ctx.request().getParam("type");
                final String id = ctx.request().getParam("id");
                
                final JsonObject data = ctx.getBodyAsJson();
                mewna.database().getOrBaseSettings(type, id).thenAccept(settings -> {
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
            });
            router.get("/data/guild/:id/levels").handler(ctx -> {
                move(() -> {
                    final String id = ctx.request().getParam("id");
                    // This makes me feel better about passing untrusted user input from the url parameters
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
                                final Player player = new JsonObject(resultSet.getString("player")).mapTo(Player.class);
                                final Account account = new JsonObject(resultSet.getString("account")).mapTo(Account.class);
                                final long userXp = player.getXp(id);
                                final long userLevel = PluginLevels.xpToLevel(userXp);
                                final long nextLevel = userLevel + 1;
                                final long currentLevelXp = PluginLevels.fullLevelToXp(userLevel);
                                final long nextLevelXp = PluginLevels.fullLevelToXp(nextLevel);
                                final long xpNeeded = PluginLevels.nextLevelXp(userXp);
                                
                                final User user = DiscordCache.user(player.getId()).toCompletableFuture().exceptionally(e -> null).join();
                                if(user != null) {
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
                                } else {
                                    results.add(new JsonObject()
                                            .put("name", "Unknown User")
                                            .put("discrim", "0001")
                                            .put("avatar", "https://cdn.discordapp.com/embed/avatars/0.png")
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
                                }
                                ++counter;
                            }
                            ctx.response().putHeader("Content-Type", "application/json")
                                    .end(new JsonArray(results).encode());
                        } else {
                            ctx.response().end("[]");
                        }
                    });
                });
            });
            router.get("/data/guild/:id/webhooks").handler(ctx -> {
                move(() -> {
                    final JsonArray arr = new JsonArray(mewna.database()
                            .getAllWebhooks(ctx.request().getParam("id"))
                            .stream()
                            .map(e -> {
                                final JsonObject j = JsonObject.mapFrom(e);
                                j.remove("secret");
                                return j;
                            })
                            .collect(Collectors.toList()));
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(arr.encode());
                });
            });
            router.get("/data/guild/:guild/webhooks/:id").handler(ctx -> {
                move(() -> {
                    final Optional<Webhook> maybeHook = mewna.database().getWebhookById(ctx.request().getParam("id"));
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(maybeHook.map(JsonObject::mapFrom).orElse(new JsonObject()).encode());
                });
            });
            router.delete("/data/guild/:guild/webhooks/:id").handler(ctx -> {
                move(() -> {
                    mewna.database().deleteWebhookById(ctx.request().getParam("id"));
                    ctx.response().end();
                });
            });
            router.get("/data/guild/:id/webhooks/:channel_id").handler(ctx -> {
                move(() -> {
                    final String channel = ctx.request().getParam("channel_id");
                    final Optional<Webhook> webhook = mewna.database().getWebhook(channel);
                    final JsonObject hook = webhook.map(JsonObject::mapFrom).orElseGet(JsonObject::new);
                    if(hook.containsKey("secret")) {
                        hook.remove("secret");
                    }
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(hook.encode());
                });
            });
            router.post("/data/guild/:id/webhooks/add").handler(BodyHandler.create()).handler(ctx -> {
                move(() -> {
                    final Webhook hook = Webhook.fromJson(ctx.getBodyAsJson());
                    mewna.database().addWebhook(hook);
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonObject().encode());
                });
            });
            
            // Levels imports
            router.post("/data/levels/import/:id/mee6").handler(ctx -> {
                final String id = ctx.request().getParam("id");
                // This makes me feel better about passing untrusted user input from the url parameters
                if(!id.matches("\\d{17,20}")) {
                    ctx.response().end(new JsonObject().encode());
                    return;
                }
                LevelsImporter.importMEE6Levels(id);
                ctx.response().end(new JsonObject().encode());
            });
            
            // Server pages
            router.post("/data/server/:id/post").handler(BodyHandler.create()).handler(ctx -> {
                // Create a post
                move(() -> {
                    ctx.response().end(new JsonObject().put("id", mewna.database()
                            .saveNewServerBlogPost(ctx.getBodyAsJson().mapTo(ServerBlogPost.class)))
                            .encode());
                });
            });
            router.get("/data/server/:id/post/:post").handler(BodyHandler.create()).handler(ctx -> {
                // Get a single post
                move(() -> {
                    ctx.response().end(mewna.database().getServerBlogPostById(ctx.request().getParam("post"))
                            .map(ServerBlogPost::toJson).orElse(new JsonObject()).encode());
                });
            });
            router.delete("/data/server/:id/post/:post").handler(BodyHandler.create()).handler(ctx -> {
                // Delete a single post
                move(() -> {
                    mewna.database().deleteServerBlogPost(ctx.request().getParam("post"));
                    ctx.response().end("");
                });
            });
            router.put("/data/server/:id/post/:post").handler(BodyHandler.create()).handler(ctx -> {
                // Edit a single post
                move(() -> {
                    ctx.response().end(new JsonObject().put("id", mewna.database()
                            .updateServerBlogPost(ctx.getBodyAsJson().mapTo(ServerBlogPost.class)))
                            .encode());
                });
            });
            router.get("/data/server/:id/posts").handler(ctx -> {
                // Get last 100 posts for a server
                move(() -> {
                    ctx.response().end(new JsonArray(mewna.database().getLast100ServerBlogPosts(ctx.request().getParam("id"))).encode());
                });
            });
            router.get("/data/server/:id/posts/all").handler(ctx -> {
                // Get all posts for a server
                move(() -> {
                    ctx.response().end(new JsonArray(mewna.database().getServerBlogPosts(ctx.request().getParam("id"))).encode());
                });
            });
            router.get("/data/server/:id/posts/all/titles").handler(ctx -> {
                // Get all posts for a server
                move(() -> {
                    ctx.response().end(mewna.database().getServerBlogPostTitles(ctx.request().getParam("id")).encode());
                });
            });
            
            router.post("/data/server/:id/post/:post/boop").handler(BodyHandler.create()).handler(ctx -> {
                // Boop a post for a given user
                // TODO
                move(() -> {
                
                });
            });
            router.delete("/data/server/:id/post/:post/boop").handler(BodyHandler.create()).handler(ctx -> {
                // Un-boop a post for a given user
                // TODO
                move(() -> {
                
                });
            });
            
            // Backgrounds
            router.get("/data/backgrounds/packs").handler(ctx -> {
                ctx.response().end(JsonObject.mapFrom(TextureManager.getPacks()).encode());
            });
            
            // Store
            router.post("/data/store/checkout/start").handler(BodyHandler.create()).handler(ctx -> {
                move(() -> {
                    final JsonObject body = ctx.getBodyAsJson();
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(mewna.paypalHandler().startPayment(
                                    body.getString("userId"),
                                    body.getString("sku")
                            ).encode());
                });
            });
            router.post("/data/store/checkout/confirm").handler(BodyHandler.create()).handler(ctx -> {
                move(() -> {
                    final JsonObject body = ctx.getBodyAsJson();
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(mewna.paypalHandler().finishPayment(
                                    body.getString("userId"),
                                    body.getString("paymentId"),
                                    body.getString("payerId")
                            ).encode());
                });
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
            router.get("/data/player/:id").handler(ctx -> {
                final String id = ctx.request().getParam("id");
                
                mewna.database().getOptionalPlayer(id)
                        .thenAccept(o -> {
                            ctx.response().putHeader("Content-Type", "application/json")
                                    .end(o
                                            .map(JsonObject::mapFrom)
                                            .orElse(new JsonObject())
                                            .encode());
                        });
            });
            
            // Votes
            router.post("/data/votes/dbl").handler(BodyHandler.create()).handler(ctx -> {
                final JsonObject body = ctx.getBodyAsJson();
                @SuppressWarnings("unused")
                final String bot = body.getString("bot");
                final String user = body.getString("user");
                final String type = body.getString("type");
                final boolean isWeekend = body.getBoolean("isWeekend");
                
                mewna.database().getOptionalPlayer(user).thenAccept(player -> {
                    if(player.isPresent()) {
                        final int amount = PluginEconomy.VOTE_BONUS * (isWeekend ? 2 : 1);
                        final Player p = player.get();
                        switch(type.toLowerCase()) {
                            case "upvote":
                            case "vote": {
                                p.setBalance(p.getBalance() + amount);
                                mewna.database().savePlayer(p);
                                mewna.statsClient().increment("votes.dbl", 1);
                                final String message;
                                if(isWeekend) {
                                    mewna.statsClient().increment("votes.dbl.weekend", 1);
                                    message = $("en_US", "votes.dbl.weekend").replace("$amount", amount + "");
                                } else {
                                    message = $("en_US", "votes.dbl.normal").replace("$amount", amount + "");
                                }
                                mewna.catnip().rest().user().createDM(user).thenAccept(channel -> channel.sendMessage(message))
                                        .thenAccept(__ -> {
                                            logger.info("Sent upvote DM to {}", user);
                                        })
                                        .exceptionally(e -> {
                                            Sentry.capture(e);
                                            return null;
                                        })
                                ;
                                break;
                            }
                            case "test": {
                                mewna.catnip().rest().user().createDM(user)
                                        .thenAccept(channel -> channel.sendMessage("```Javascript\n" + body.encodePrettily() + "\n```"));
                                break;
                            }
                        }
                    }
                });
                ctx.response().end("{}");
            });
        }
        
        server.requestHandler(router::accept).listen(mewna.port());
    }
}
