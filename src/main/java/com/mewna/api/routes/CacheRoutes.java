package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.catnip.entity.channel.Channel;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.data.DiscordCache;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author amy
 * @since 2/10/19.
 */
public class CacheRoutes implements RouteGroup {
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
    
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.get("/cache/user/:id").handler(ctx -> {
            final String id = ctx.request().getParam("id");
            DiscordCache.user(id)
                    .thenAccept(user -> ctx.response().putHeader("Content-Type", "application/json")
                            .end(JsonObject.mapFrom(user).put("id", user.id()).encode()))
                    .exceptionally(e -> {
                        ctx.response().putHeader("Content-Type", "application/json")
                                .setStatusCode(404)
                                .end(new JsonObject().encode());
                        return null;
                    });
        });
        router.get("/cache/guild/:id").handler(ctx -> {
            final String id = ctx.request().getParam("id");
            DiscordCache.guild(id)
                    .thenAccept(guild -> ctx.response().putHeader("Content-Type", "application/json")
                            .end(JsonObject.mapFrom(guild).put("id", guild.id()).encode()))
                    .exceptionally(e -> {
                        ctx.response().putHeader("Content-Type", "application/json")
                                .setStatusCode(404)
                                .end(new JsonObject().encode());
                        return null;
                    });
        });
        router.get("/cache/guild/:id/channels").handler(ctx -> {
            final String id = ctx.request().getParam("id");
            DiscordCache.channels(id)
                    .thenAccept(channels -> ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonArray(minifyChannels(channels)).encode()))
                    .exceptionally(e -> {
                        ctx.response().putHeader("Content-Type", "application/json")
                                .setStatusCode(404)
                                .end(new JsonArray().encode());
                        return null;
                    });
        });
        router.get("/cache/guild/:id/roles").handler(ctx -> {
            final String id = ctx.request().getParam("id");
            DiscordCache.roles(id)
                    .thenAccept(roles -> ctx.response().putHeader("Content-Type", "application/json")
                            .end(new JsonArray(minifyRoles(roles)).encode()))
                    .exceptionally(e -> {
                        ctx.response().putHeader("Content-Type", "application/json")
                                .setStatusCode(404)
                                .end(new JsonArray().encode());
                        return null;
                    });
        });
    }
}
