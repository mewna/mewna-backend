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
import io.sentry.Sentry;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.Value;
import lombok.experimental.Accessors;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author amy
 * @since 10/31/18.
 */
@SuppressWarnings("unused")
public final class DiscordCache {
    private static final Map<String, PendingFetch<?>> PENDING = new ConcurrentHashMap<>();
    
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
    
    private static <T> CompletionStage<T> fetch(@Nonnull final Class<T> cls, @Nonnull final String mode,
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
    
    public static CompletionStage<Role> role(final String guild, final String id) {
        return fetch(Role.class, "role", new JsonObject().put("guild", guild).put("id", id));
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
