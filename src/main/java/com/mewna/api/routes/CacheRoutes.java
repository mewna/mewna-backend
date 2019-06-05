package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.catnip.entity.channel.Channel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.util.ImageOptions;
import com.mewna.data.cache.DiscordCache;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.stream.Collectors;

import static com.mewna.util.Async.move;

/**
 * @author amy
 * @since 4/11/19.
 */
public class CacheRoutes implements RouteGroup {
    private JsonObject minify(final Role e) {
        return new JsonObject()
                .put("id", e.id())
                .put("guildId", e.guildId())
                .put("name", e.name())
                .put("position", e.position())
                .put("color", e.color())
                .put("managed", e.managed());
    }
    
    private JsonObject minify(final Channel channel) {
        return new JsonObject()
                .put("id", channel.id())
                .put("type", channel.type().getKey())
                .put("guildId", channel.asGuildChannel().guildId())
                .put("name", channel.asGuildChannel().name())
                .put("position", channel.asGuildChannel().position())
                .put("nsfw", channel.isText() && channel.asTextChannel().nsfw());
    }
    
    private JsonObject minify(final User user) {
        if(user == null) {
            return new JsonObject();
        }
        return new JsonObject()
                .put("id", user.id())
                .put("username", user.username())
                .put("discriminator", user.discriminator())
                .put("avatar", user.effectiveAvatarUrl(new ImageOptions().png().size(128)))
                .put("bot", user.bot());
    }
    
    private JsonObject minify(final Member member) {
        if(member == null) {
            return new JsonObject();
        }
        return new JsonObject()
                .put("id", member.id())
                .put("guild", member.guildId())
                .put("roles", member.roleIds())
                ;
    }
    
    private JsonObject minify(final Guild guild) {
        if(guild == null) {
            return new JsonObject();
        }
        return new JsonObject()
                .put("id", guild.id())
                .put("name", guild.name())
                .put("icon", guild.iconUrl(new ImageOptions().png().size(128)));
    }
    
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.get("/v3/cache/user/:id").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final User user = DiscordCache.user(id);
            ctx.response().end(minify(user).encode());
        }));
        router.get("/v3/cache/guild/:id").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final Guild guild = DiscordCache.guild(id);
            ctx.response().end(minify(guild).encode());
        }));
        router.get("/v3/cache/guild/:id/exists").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final Guild guild = DiscordCache.guild(id);
            final var out = new JsonObject().put("exists", guild != null);
            ctx.response().end(out.encode());
        }));
        router.get("/v3/cache/guild/:id/roles").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final var roles = DiscordCache.roles(id).stream()
                    .map(this::minify)
                    .collect(Collectors.toList());
            ctx.response().end(new JsonArray(roles).encode());
        }));
        router.get("/v3/cache/guild/:id/channels").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final var channels = DiscordCache.channels(id).stream()
                    .map(this::minify)
                    .collect(Collectors.toList());
            ctx.response().end(new JsonArray(channels).encode());
        }));
    }
}
