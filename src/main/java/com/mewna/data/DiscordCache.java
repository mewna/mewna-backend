package com.mewna.data;

import com.mewna.Mewna;
import com.mewna.catnip.entity.Entity;
import com.mewna.catnip.entity.channel.TextChannel;
import com.mewna.catnip.entity.channel.VoiceChannel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.user.VoiceState;
import gg.amy.singyeong.QueryBuilder;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import lombok.Value;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.mewna.util.MewnaFutures.block;

/**
 * TODO: Implement a read-only singyeong-backed catnip cache worker
 *
 * @author amy
 * @since 10/31/18.
 */
@SuppressWarnings("unused")
public final class DiscordCache {
    private static final Map<String, PendingFetch<?>> PENDING = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordCache.class);
    
    private static boolean SETUP;
    
    private DiscordCache() {
    }
    
    public static Guild guild(final String id) {
        final var buffer = block(Mewna.getInstance().singyeong().proxy(HttpMethod.GET,
                "/cache/guild/" + id, "shards",
                new QueryBuilder().build()));
        if(buffer.toString().equalsIgnoreCase("{}")) {
            return null;
        }
        return Entity.fromJson(Mewna.getInstance().catnip(), Guild.class, buffer.toJsonObject());
    }
    
    public static User user(final String id) {
        final var buffer = block(Mewna.getInstance().singyeong().proxy(HttpMethod.GET,
                "/cache/user/" + id, "shards",
                new QueryBuilder().build()));
        if(buffer.toString().equalsIgnoreCase("{}")) {
            return null;
        }
        return Entity.fromJson(Mewna.getInstance().catnip(), User.class, buffer.toJsonObject());
    }
    
    public static TextChannel textChannel(final String guild, final String id) {
        final var buffer = block(Mewna.getInstance().singyeong().proxy(HttpMethod.GET,
                "/cache/guild/" + guild + "/channels/text/" + id, "shards",
                new QueryBuilder().build()));
        if(buffer.toString().equalsIgnoreCase("{}")) {
            return null;
        }
        return Entity.fromJson(Mewna.getInstance().catnip(), TextChannel.class, buffer.toJsonObject());
    }
    
    public static VoiceChannel voiceChannel(final String guild, final String id) {
        final var buffer = block(Mewna.getInstance().singyeong().proxy(HttpMethod.GET,
                "/cache/guild/" + guild + "/channels/voice/" + id, "shards",
                new QueryBuilder().build()));
        if(buffer.toString().equalsIgnoreCase("{}")) {
            return null;
        }
        return Entity.fromJson(Mewna.getInstance().catnip(), VoiceChannel.class, buffer.toJsonObject());
    }
    
    public static Collection<TextChannel> channels(final String guild) {
        final var buffer = block(Mewna.getInstance().singyeong().proxy(HttpMethod.GET,
                "/cache/guild/" + guild + "/channels/text", "shards",
                new QueryBuilder().build()));
        if(buffer.toString().equalsIgnoreCase("[]")) {
            return new ArrayList<>();
        }
        return buffer.toJsonArray().stream()
                .map(e -> (JsonObject) e)
                .map(e -> Entity.fromJson(Mewna.getInstance().catnip(), TextChannel.class, e))
                .collect(Collectors.toList());
    }
    
    public static Role role(final String guild, final String id) {
        final var buffer = block(Mewna.getInstance().singyeong().proxy(HttpMethod.GET,
                "/cache/guild/" + guild + "/roles/" + id, "shards",
                new QueryBuilder().build()));
        if(buffer.toString().equalsIgnoreCase("{}")) {
            return null;
        }
        return Entity.fromJson(Mewna.getInstance().catnip(), Role.class, buffer.toJsonObject());
    }
    
    public static Collection<Role> roles(final String guild) {
        final var buffer = block(Mewna.getInstance().singyeong().proxy(HttpMethod.GET,
                "/cache/guild/" + guild + "/roles", "shards",
                new QueryBuilder().build()));
        if(buffer.toString().equalsIgnoreCase("[]")) {
            return new ArrayList<>();
        }
        return buffer.toJsonArray().stream()
                .map(e -> (JsonObject) e)
                .map(e -> Entity.fromJson(Mewna.getInstance().catnip(), Role.class, e))
                .collect(Collectors.toList());
    }
    
    public static Member member(final String guild, final String id) {
        final var buffer = block(Mewna.getInstance().singyeong().proxy(HttpMethod.GET,
                "/cache/guild/" + guild + "/member/" + id, "shards",
                new QueryBuilder().build()));
        if(buffer.toString().equalsIgnoreCase("{}")) {
            return null;
        }
        return Entity.fromJson(Mewna.getInstance().catnip(), Member.class, buffer.toJsonObject());
    }
    
    public static VoiceState voiceState(final String guild, final String id) {
        final var buffer = block(Mewna.getInstance().singyeong().proxy(HttpMethod.GET,
                "/cache/guild/" + guild + "/member/" + id + "/voiceState", "shards",
                new QueryBuilder().build()));
        if(buffer.toString().equalsIgnoreCase("{}")) {
            return null;
        }
        return Entity.fromJson(Mewna.getInstance().catnip(), VoiceState.class, buffer.toJsonObject());
    }
    
    @Value
    @Accessors(fluent = true)
    private static final class PendingFetch<T> {
        private Class<T> cls;
        private Future<T> future;
    }
}
