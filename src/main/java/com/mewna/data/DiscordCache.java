package com.mewna.data;

import com.mewna.Mewna;
import com.mewna.catnip.entity.Entity;
import com.mewna.catnip.entity.channel.Channel;
import com.mewna.catnip.entity.channel.Channel.ChannelType;
import com.mewna.catnip.entity.channel.GuildChannel;
import com.mewna.catnip.entity.channel.TextChannel;
import com.mewna.catnip.entity.channel.VoiceChannel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.user.VoiceState;
import gg.amy.singyeong.QueryBuilder;
import io.sentry.Sentry;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.Value;
import lombok.experimental.Accessors;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    
    public static void setup() {
        if(SETUP) {
            return;
        }
        SETUP = true;
        Mewna.getInstance().singyeong().onEvent(dispatch -> {
            final String nonce = dispatch.nonce();
            if(nonce != null && PENDING.containsKey(nonce)) {
                final PendingFetch p = PENDING.remove(nonce);
                final JsonObject data = dispatch.data();
                if(data.isEmpty()) {
                    p.future.fail("Not available in cache!");
                } else if(data.containsKey("_type")) {
                    final JsonArray array = data.getJsonArray("_data");
                    switch(data.getString("_type")) {
                        case "channels": {
                            final List<Channel> channels = array.stream()
                                    .map(e -> (JsonObject) e)
                                    .filter(e -> {
                                        final ChannelType channelType = ChannelType.valueOf(e.getString("type"));
                                        return channelType == ChannelType.TEXT || channelType == ChannelType.VOICE;
                                    })
                                    .map(e -> {
                                        final ChannelType channelType = ChannelType.valueOf(e.getString("type"));
                                        if(channelType == ChannelType.TEXT) {
                                            return Entity.fromJson(Mewna.getInstance().catnip(), TextChannel.class, e);
                                        } else {
                                            return Entity.fromJson(Mewna.getInstance().catnip(), VoiceChannel.class, e);
                                        }
                                    })
                                    .collect(Collectors.toList());
                            //noinspection unchecked
                            p.future.complete(channels);
                            break;
                        }
                        case "roles": {
                            final List<Role> channels = array.stream()
                                    .map(e -> (JsonObject) e)
                                    .map(e -> Entity.fromJson(Mewna.getInstance().catnip(), Role.class, e))
                                    .collect(Collectors.toList());
                            //noinspection unchecked
                            p.future.complete(channels);
                            break;
                        }
                        default: {
                            LOGGER.warn("Unknown cache collection type {}", data.getString("_type"));
                        }
                    }
                } else {
                    try {
                        // y i k e s
                        //noinspection unchecked
                        p.future.complete(Entity.fromJson(Mewna.getInstance().catnip(), p.cls, data));
                    } catch(final Exception e) {
                        p.future.fail(e);
                        Sentry.capture(e);
                    }
                }
            }
        });
    }
    
    private static <T> CompletionStage<T> fetch(@Nullable final Class<T> cls, @Nonnull final String mode,
                                                @Nonnull final JsonObject query) {
        final Future<T> future = Future.future();
        final String nonce = UUID.randomUUID().toString();
        final PendingFetch<T> pending = new PendingFetch<>(cls, future);
        
        Mewna.getInstance().singyeong().send("mewna-shard", nonce, new QueryBuilder().build(),
                new JsonObject()
                        .put("type", "CACHE")
                        .put("query", query.put("mode", mode)));
        
        PENDING.putIfAbsent(nonce, pending);
        return VertxCompletableFuture.from(Mewna.getInstance().vertx(), future);
    }
    
    public static CompletionStage<Guild> guild(final String id) {
        return fetch(Guild.class, "guild", new JsonObject().put("id", id));
    }
    
    public static CompletionStage<User> user(final String id) {
        return fetch(User.class, "user", new JsonObject().put("id", id));
    }
    
    public static CompletionStage<TextChannel> textChannel(final String guild, final String id) {
        return fetch(TextChannel.class, "channel", new JsonObject().put("guild", guild).put("id", id));
    }
    
    public static CompletionStage<VoiceChannel> voiceChannel(final String guild, final String id) {
        return fetch(VoiceChannel.class, "channel", new JsonObject().put("guild", guild).put("id", id));
    }
    
    public static CompletionStage<Collection<Channel>> channels(final String guild) {
        return fetch(null, "channels", new JsonObject().put("id", guild));
    }
    
    public static CompletionStage<Role> role(final String guild, final String id) {
        return fetch(Role.class, "role", new JsonObject().put("guild", guild).put("id", id));
    }
    
    public static CompletionStage<Collection<Role>> roles(final String guild) {
        return fetch(null, "roles", new JsonObject().put("id", guild));
    }
    
    public static CompletionStage<Member> member(final String guild, final String id) {
        return fetch(Member.class, "member", new JsonObject().put("guild", guild).put("id", id));
    }
    
    public static CompletionStage<VoiceState> voiceState(final String guild, final String id) {
        return fetch(VoiceState.class, "voice-state", new JsonObject().put("guild", guild).put("id", id));
    }
    
    @Value
    @Accessors(fluent = true)
    private static final class PendingFetch<T> {
        private Class<T> cls;
        private Future<T> future;
    }
}
