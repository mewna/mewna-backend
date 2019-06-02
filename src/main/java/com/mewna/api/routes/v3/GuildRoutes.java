package com.mewna.api.routes.v3;

import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.api.RouteGroup;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.catnip.entity.user.User;
import com.mewna.data.Player;
import com.mewna.data.Server;
import com.mewna.data.Webhook;
import com.mewna.data.cache.DiscordCache;
import com.mewna.plugin.plugins.PluginLevels;
import com.mewna.plugin.plugins.settings.BehaviourSettings;
import com.mewna.plugin.plugins.settings.LevelsSettings;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.mewna.util.Async.move;
import static com.mewna.util.MewnaFutures.block;

/**
 * @author amy
 * @since 4/10/19.
 */
public class GuildRoutes implements RouteGroup {
    private JsonObject minify(final Role e) {
        return new JsonObject()
                .put("id", e.id())
                .put("guildId", e.guildId())
                .put("name", e.name())
                .put("position", e.position())
                .put("color", e.color())
                .put("managed", e.managed());
    }
    
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.get("/v3/guild/:id").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final Server server = mewna.database().getServer(id);
            ctx.response().end(JsonObject.mapFrom(server).encode());
        }));
        router.post("/v3/guild/:id").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final Server prev = mewna.database().getServer(id);
            final Server server = ctx.getBodyAsJson().mapTo(Server.class);
            if(server.validate(prev)) {
                server.save(mewna.database(), prev);
                ctx.response().end(new JsonObject().encode());
            } else {
                ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("invalid server config"))).encode());
            }
        }));
        
        router.get("/v3/guild/:id/rewards").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final var roles = DiscordCache.roles(id);
            if(roles != null) {
                final var settings = block(mewna.database().getOrBaseSettings(LevelsSettings.class, id));
                final var rewards = roles.stream()
                        .filter(e -> settings.getLevelRoleRewards().containsKey(e.id()))
                        .map(e -> new JsonObject().put("level", settings.getLevelRoleRewards().get(e.id())).put("role", minify(e)))
                        .sorted(Comparator.comparingLong(e -> e.getLong("level")))
                        .collect(Collectors.toList());
                ctx.response().end(new JsonArray(rewards).encode());
            } else {
                ctx.response().end(new JsonArray().encode());
            }
        }));
        
        router.get("/v3/guild/:id/webhooks").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final var webhooks = mewna.database().getAllWebhooks(id).stream()
                    .map(JsonObject::mapFrom)
                    .peek(e -> e.remove("secret"))
                    .collect(Collectors.toList());
            ctx.response().end(new JsonArray(webhooks).encode());
        }));
        
        router.delete("/v3/guild/:guild/webhooks/:id").handler(ctx -> move(() -> {
            mewna.database().deleteWebhookById(ctx.request().getParam("id"));
            // TODO: Check return values etc
            ctx.response().end(new JsonObject().encode());
        }));
        
        router.post("/v3/guild/:id/webhooks/add").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final Webhook webhook = Webhook.fromJson(ctx.getBodyAsJson());
            mewna.database().addWebhook(webhook);
            // TODO: Check return values etc
            ctx.response().end(new JsonObject().encode());
        }));
        
        router.get("/v3/guild/:id/prefix").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final var roles = DiscordCache.roles(id);
            if(roles != null) {
                final var settings = block(mewna.database().getOrBaseSettings(BehaviourSettings.class, id));
                ctx.response().end(new JsonObject().put("prefix", settings.getPrefix()).encode());
            } else {
                ctx.response().end(new JsonObject().encode());
            }
        }));
        
        router.get("/v3/guild/:id/leaderboard").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final List<String> afterParam = ctx.queryParam("after");
            final long after;
            if(afterParam == null || afterParam.isEmpty()) {
                after = 0L;
            } else {
                after = Long.parseLong(afterParam.get(0));
            }
            // This makes me feel better about passing untrusted user input from the url parameters
            if(!id.matches("\\d{17,20}")) {
                ctx.response().end(new JsonArray().encode());
                return;
            }
            // This is gonna be ugly
            final List<JsonObject> results = new ArrayList<>();
            
            final String query = String.format(
                    "SELECT * FROM (\n" +
                            "    SELECT\n" +
                            "        row_number() OVER (\n" +
                            "            PARTITION BY (players.data->'guildXp' ?? '%s')\n" +
                            "            ORDER BY (players.data->'guildXp'->>'%s')::integer DESC\n" +
                            "        ) AS rank,\n" +
                            "        players.data AS player,\n" +
                            "        accounts.data AS account\n" +
                            "    FROM players\n" +
                            "    INNER JOIN accounts ON accounts.id = players.id\n" +
                            "    WHERE\n" +
                            "        players.data->'guildXp' ?? '%s'\n" +
                            "        AND (players.data->'guildXp'->>'%s')::integer > 0\n" +
                            "    ORDER BY (players.data->'guildXp'->>'%s')::integer DESC\n" +
                            ")\n" +
                            "AS _\n" +
                            "WHERE rank > %s\n" +
                            "LIMIT 100;",
                    id, id, id, id, id, after
            );
            
            mewna.database().getStore().sql(query, p -> {
                final ResultSet resultSet = p.executeQuery();
                if(resultSet.isBeforeFirst()) {
                    while(resultSet.next()) {
                        final Player player = new JsonObject(resultSet.getString("player")).mapTo(Player.class);
                        final Account account = new JsonObject(resultSet.getString("account")).mapTo(Account.class);
                        final long userXp = player.getXp(id);
                        final long userLevel = PluginLevels.xpToLevel(userXp);
                        final long nextLevel = userLevel + 1;
                        final long currentLevelXp = PluginLevels.fullLevelToXp(userLevel);
                        final long nextLevelXp = PluginLevels.fullLevelToXp(nextLevel);
                        final long xpNeeded = PluginLevels.nextLevelXp(userXp);
                        
                        final User user = DiscordCache.user(player.getId());
                        if(user != null) {
                            results.add(new JsonObject()
                                    .put("name", user.username())
                                    .put("discrim", user.discriminator())
                                    .put("avatar", user.effectiveAvatarUrl())
                                    .put("userXp", userXp)
                                    .put("userLevel", userLevel)
                                    .put("nextLevel", nextLevel)
                                    .put("playerRank", (long) resultSet.getInt("rank"))
                                    .put("currentLevelXp", currentLevelXp)
                                    .put("xpNeeded", xpNeeded)
                                    .put("nextLevelXp", nextLevelXp)
                                    .put("customBackground", account.customBackground())
                                    .put("accountId", player.getId())
                            );
                        } else {
                            results.add(new JsonObject()
                                    .put("name", "Unknown User")
                                    .put("discrim", "0000")
                                    .put("avatar", "https://cdn.discordapp.com/embed/avatars/0.png")
                                    .put("userXp", userXp)
                                    .put("userLevel", userLevel)
                                    .put("nextLevel", nextLevel)
                                    .put("playerRank", (long) resultSet.getInt("rank"))
                                    .put("currentLevelXp", currentLevelXp)
                                    .put("xpNeeded", xpNeeded)
                                    .put("nextLevelXp", nextLevelXp)
                                    .put("customBackground", account.customBackground())
                                    .put("accountId", player.getId())
                            );
                        }
                    }
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonArray(results).encode());
                } else {
                    ctx.response().end("[]");
                }
            });
        }));
    }
}
