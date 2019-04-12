package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amy
 * @since 2/10/19.
 */
public class GuildRoutes implements RouteGroup {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        /*
        router.get("/data/guild/:id/config/:type").handler(ctx -> {
            final String id = ctx.request().getParam("id");
            final String type = ctx.request().getParam("type");
            mewna.database().getOrBaseSettings(type, id).thenAccept(settings ->
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(JsonObject.mapFrom(settings).encode()));
        });
        router.post("/data/guild/:id/config/:type")
                .handler(BodyHandler.create())
                .handler(ctx -> move(() ->{
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
                }));
        router.get("/data/guild/:id/levels").handler(ctx -> move(() -> {
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
                                    .put("accountId", player.getId())
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
                                    .put("accountId", player.getId())
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
        }));
        router.get("/data/guild/:id/webhooks").handler(ctx -> move(() -> {
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
        }));
        router.get("/data/guild/:guild/webhooks/:id").handler(ctx -> move(() -> {
            final Optional<Webhook> maybeHook = mewna.database().getWebhookById(ctx.request().getParam("id"));
            ctx.response().putHeader("Content-Type", "application/json")
                    .end(maybeHook.map(JsonObject::mapFrom).orElse(new JsonObject()).encode());
        }));
        router.delete("/data/guild/:guild/webhooks/:id").handler(ctx -> move(() -> {
            mewna.database().deleteWebhookById(ctx.request().getParam("id"));
            ctx.response().end();
        }));
        router.get("/data/guild/:id/webhooks/:channel_id").handler(ctx -> move(() -> {
            final String channel = ctx.request().getParam("channel_id");
            final Optional<Webhook> webhook = mewna.database().getWebhook(channel);
            final JsonObject hook = webhook.map(JsonObject::mapFrom).orElseGet(JsonObject::new);
            if(hook.containsKey("secret")) {
                hook.remove("secret");
            }
            ctx.response().putHeader("Content-Type", "application/json")
                    .end(hook.encode());
        }));
        router.post("/data/guild/:id/webhooks/add")
                .handler(BodyHandler.create())
                .handler(ctx -> move(() -> {
                    final Webhook hook = Webhook.fromJson(ctx.getBodyAsJson());
                    mewna.database().addWebhook(hook);
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonObject().encode());
                }));
                */
    }
}
