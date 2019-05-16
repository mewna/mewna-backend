package com.mewna.data.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.catnip.Catnip;
import com.mewna.catnip.cache.CacheFlag;
import com.mewna.catnip.cache.CustomizableEntityCache;
import com.mewna.catnip.cache.EntityCache;
import com.mewna.catnip.cache.view.DefaultNamedCacheView;
import com.mewna.catnip.cache.view.NamedCacheView;
import com.mewna.catnip.entity.Snowflake;
import com.mewna.catnip.entity.channel.Channel;
import com.mewna.catnip.entity.channel.GuildChannel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.catnip.entity.impl.EntityBuilder;
import com.mewna.catnip.entity.user.Presence;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.user.VoiceState;
import com.mewna.catnip.shard.DiscordEvent.Raw;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.sentry.Sentry;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author amy
 * @since 4/28/19.
 */
@SuppressWarnings({"WeakerAccess", "FieldCanBeLocal", "unused"})
public class MsgPackRedisCache extends CustomizableEntityCache {
    public static final String USER_HASH = "mewna:discord:cache:users";
    public static final String GUILD_HASH = "mewna:discord:cache:guilds";
    public static final String GUILD_CHANNEL_HASH = "mewna:discord:cache:guild:%s:channels";
    public static final String GUILD_ROLE_HASH = "mewna:discord:cache:guild:%s:roles";
    public static final String GUILD_MEMBER_HASH = "mewna:discord:cache:guild:%s:members";
    public static final String GUILD_VOICE_STATES_HASH = "mewna:discord:cache:guild:%s:voice-states";
    
    private final ObjectMapper packer = new ObjectMapper(new MessagePackFactory());
    private final RedisClient client;
    private final StatefulRedisConnection<String, byte[]> connection;
    private final RedisAsyncCommands<String, byte[]> redis;
    private EntityBuilder entityBuilder;
    
    public MsgPackRedisCache(final String dsn) {
        client = RedisClient.create(dsn);
        connection = client.connect(new StringToBytesCodec());
        redis = connection.async();
    }
    
    @Nonnull
    @Override
    public Future<Void> updateCache(@Nonnull final String eventType, final int shardId, @Nonnull final JsonObject payload) {
        switch(eventType) {
            // Lifecycle
            case Raw.READY: {
                break;
            }
            // Channels
            case Raw.CHANNEL_CREATE:
            case Raw.CHANNEL_UPDATE: {
                final Channel channel = entityBuilder.createChannel(payload);
                if(channel.isGuild()) {
                    final GuildChannel gc = (GuildChannel) channel;
                    final String key = String.format(GUILD_CHANNEL_HASH, gc.guildId());
                    cache(key, gc);
                }
                break;
            }
            case Raw.CHANNEL_DELETE: {
                final Channel channel = entityBuilder.createChannel(payload);
                if(channel.isGuild()) {
                    final GuildChannel gc = (GuildChannel) channel;
                    final String key = String.format(GUILD_CHANNEL_HASH, gc.guildId());
                    uncache(key, gc);
                }
                break;
            }
            // Guilds
            case Raw.GUILD_CREATE: {
                // This is wrapped in a blocking executor because there could
                // be cases of massive guilds that end blocking for a
                // significant amount of time while the guild is being cached.
                final Future<Void> future = Future.future();
                catnip.vertx().executeBlocking(f -> {
                    final Guild guild = entityBuilder.createAndCacheGuild(shardId, payload);
                    cache(GUILD_HASH, guild);
                    f.complete(null);
                }, __ -> future.complete(null));
                return future;
            }
            case Raw.GUILD_UPDATE: {
                final Future<Void> future = Future.future();
                catnip.vertx().executeBlocking(f -> {
                    final Guild guild = entityBuilder.createGuild(payload);
                    cache(GUILD_HASH, guild);
                    f.complete(null);
                }, __ -> future.complete(null));
                return future;
            }
            case Raw.GUILD_DELETE: {
                final var guildId = payload.getString("id");
                
                uncache(GUILD_HASH, guildId);
                for(final String hash : new String[] {
                        GUILD_CHANNEL_HASH,
                        GUILD_ROLE_HASH,
                        GUILD_MEMBER_HASH,
                        GUILD_VOICE_STATES_HASH
                }) {
                    final String key = String.format(hash, guildId);
                    uncache(key, guildId);
                }
                
                break;
            }
            // Roles
            case Raw.GUILD_ROLE_CREATE: {
                final String guild = payload.getString("guild_id");
                final JsonObject json = payload.getJsonObject("role");
                final Role role = entityBuilder.createRole(guild, json);
                final String key = String.format(GUILD_ROLE_HASH, guild);
                cache(key, role);
                break;
            }
            case Raw.GUILD_ROLE_UPDATE: {
                final String guild = payload.getString("guild_id");
                final JsonObject json = payload.getJsonObject("role");
                final Role role = entityBuilder.createRole(guild, json);
                final String key = String.format(GUILD_ROLE_HASH, guild);
                cache(key, role);
                break;
            }
            case Raw.GUILD_ROLE_DELETE: {
                final String guild = payload.getString("guild_id");
                final String role = payload.getString("role_id");
                final String key = String.format(GUILD_ROLE_HASH, guild);
                uncache(key, role);
                break;
            }
            // Members
            case Raw.GUILD_MEMBER_ADD: {
                final Member member = entityBuilder.createMember(payload.getString("guild_id"), payload);
                final User user = entityBuilder.createUser(payload.getJsonObject("user"));
                cache(USER_HASH, user);
                final String key = String.format(GUILD_MEMBER_HASH, member.guildId());
                cache(key, member);
                break;
            }
            case Raw.GUILD_MEMBER_UPDATE: {
                // This doesn't send an object like all the other events, so we build a fake
                // payload object and create an entity from that
                final JsonObject user = payload.getJsonObject("user");
                final String id = user.getString("id");
                final String guild = payload.getString("guild_id");
                final Member old = member(guild, id);
                if(old != null) {
                    @SuppressWarnings("ConstantConditions")
                    final JsonObject data = new JsonObject()
                            .put("user", user)
                            .put("roles", payload.getJsonArray("roles"))
                            .put("nick", payload.getString("nick"))
                            .put("deaf", old.deaf())
                            .put("mute", old.mute())
                            .put("joined_at", old.joinedAt()
                                    // If we have an old member cached, this shouldn't be an issue
                                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                    final Member member = entityBuilder.createMember(guild, data);
                    final String key = String.format(GUILD_MEMBER_HASH, member.guildId());
                    cache(key, member);
                } else {
                    catnip.logAdapter().warn("Got GUILD_MEMBER_UPDATE for {} in {}, but we don't have them cached?!", id, guild);
                }
                break;
            }
            case Raw.GUILD_MEMBER_REMOVE: {
                final String guild = payload.getString("guild_id");
                final String user = payload.getJsonObject("user").getString("id");
                final String key = String.format(GUILD_MEMBER_HASH, guild);
                uncache(key, user);
                break;
            }
            // Member chunking
            case Raw.GUILD_MEMBERS_CHUNK: {
                final String guild = payload.getString("guild_id");
                final JsonArray members = payload.getJsonArray("members");
                final String key = String.format(GUILD_MEMBER_HASH, guild);
                members.stream().map(e -> entityBuilder.createMember(guild, (JsonObject) e)).forEach(e -> cache(key, e));
                break;
            }
            // Emojis
            case Raw.GUILD_EMOJIS_UPDATE: {
                break;
            }
            // Currently-logged-in user
            case Raw.USER_UPDATE: {
                break;
            }
            // Users
            case Raw.PRESENCE_UPDATE: {
                final JsonObject user = payload.getJsonObject("user");
                final String id = user.getString("id");
                final User old = user(id);
                if(old == null && !catnip.chunkMembers() && catnip.logUncachedPresenceWhenNotChunking()) {
                    catnip.logAdapter().warn("Received PRESENCE_UPDATE for uncached user {}!?", id);
                } else if(old != null) {
                    // This could potentially update:
                    // - username
                    // - discriminator
                    // - avatar
                    // so we check the existing cache for a user, and update as needed
                    final User updated = entityBuilder.createUser(new JsonObject()
                            .put("id", id)
                            .put("bot", old.bot())
                            .put("username", user.getString("username", old.username()))
                            .put("discriminator", user.getString("discriminator", old.discriminator()))
                            .put("avatar", user.getString("avatar", old.avatar()))
                    );
                    cache(USER_HASH, updated);
                    // TODO: Presence caching here?
                } else if(catnip.chunkMembers()) {
                    catnip.logAdapter().warn("Received PRESENCE_UPDATE for unknown user {}!? (member chunking enabled)", id);
                }
                break;
            }
            // Voice
            case Raw.VOICE_STATE_UPDATE: {
                if(!catnip.cacheFlags().contains(CacheFlag.DROP_VOICE_STATES)) {
                    final VoiceState state = entityBuilder.createVoiceState(payload);
                    final String key = String.format(GUILD_VOICE_STATES_HASH, state.guildId());
                    // TODO: How to un-cache these? Should we even?
                    cache(key, state);
                }
                break;
            }
        }
        // Default case; most events don't need to have special future cases
        return Future.succeededFuture();
    }
    
    @Override
    public void bulkCacheUsers(final int shardId, @Nonnull final Collection<User> users) {
        users.forEach(e -> cache(USER_HASH, e));
    }
    
    @Override
    public void bulkCacheChannels(final int shardId, @Nonnull final Collection<GuildChannel> channels) {
        if(channels.isEmpty()) {
            return;
        }
        final String guildId = channels.stream().findFirst().get().guildId();
        final String key = String.format(GUILD_CHANNEL_HASH, guildId);
        channels.forEach(e -> cache(key, e));
    }
    
    @Override
    public void bulkCacheRoles(final int shardId, @Nonnull final Collection<Role> roles) {
        if(roles.isEmpty()) {
            return;
        }
        final String guildId = roles.stream().findFirst().get().guildId();
        final String key = String.format(GUILD_ROLE_HASH, guildId);
        roles.forEach(e -> cache(key, e));
    }
    
    @Override
    public void bulkCacheMembers(final int shardId, @Nonnull final Collection<Member> members) {
        if(members.isEmpty()) {
            return;
        }
        final String guildId = members.stream().findFirst().get().guildId();
        final String key = String.format(GUILD_MEMBER_HASH, guildId);
        members.forEach(e -> cache(key, e));
    }
    
    @Override
    public void bulkCacheVoiceStates(final int shardId, @Nonnull final Collection<VoiceState> voiceStates) {
        if(voiceStates.isEmpty()) {
            return;
        }
        final String guildId = voiceStates.stream().findFirst().get().guildId();
        final String key = String.format(GUILD_VOICE_STATES_HASH, guildId);
        voiceStates.forEach(e -> cache(key, e));
    }
    
    @Override
    public void bulkCachePresences(final int shardId, @Nonnull final Map<String, Presence> presences) {
        // TODO: Do I want to store presences? If yes, what parts?
    }
    
    @Override
    public void invalidateShard(final int id) {
        // TODO: Is this even needed?
    }
    
    @Nonnull
    @Override
    public CompletableFuture<Guild> guildAsync(final long id) {
        return read(GUILD_HASH, id, Guild.class);
    }
    
    @Nonnull
    @Override
    public CompletableFuture<User> userAsync(final long id) {
        return read(USER_HASH, id, User.class);
    }
    
    @Nonnull
    @Override
    public CompletableFuture<Presence> presenceAsync(final long id) {
        // TODO: Do I really wanna do this?
        return super.presenceAsync(id);
    }
    
    @Nonnull
    @Override
    public CompletableFuture<Member> memberAsync(final long guildId, final long id) {
        final String key = String.format(GUILD_MEMBER_HASH, guildId);
        return read(key, id, Member.class);
    }
    
    @Nonnull
    @Override
    public CompletableFuture<Role> roleAsync(final long guildId, final long id) {
        final String key = String.format(GUILD_ROLE_HASH, guildId);
        return read(key, id, Role.class);
    }
    
    @Nonnull
    @Override
    public CompletableFuture<GuildChannel> channelAsync(final long guildId, final long id) {
        final String key = String.format(GUILD_CHANNEL_HASH, guildId);
        return read(key, id, GuildChannel.class);
    }
    
    @Nonnull
    @Override
    public CompletableFuture<VoiceState> voiceStateAsync(final long guildId, final long id) {
        final String key = String.format(GUILD_VOICE_STATES_HASH, guildId);
        return read(key, id, VoiceState.class);
    }
    
    @Nonnull
    @Override
    public NamedCacheView<Role> roles(final long guildId) {
        final String key = String.format(GUILD_ROLE_HASH, guildId);
        final RedisFuture<Map<String, byte[]>> future = redis.hgetall(key);
        try {
            final var roles = new DefaultNamedCacheView<>(Role::name);
            future.await(5, TimeUnit.SECONDS);
            final Map<String, byte[]> map = future.get();
            map.forEach((id, bytes) -> {
                final var deser = deserialize(bytes, Role.class);
                if(deser != null) {
                    roles.put(Long.parseUnsignedLong(id), deser);
                }
            });
            return roles;
        } catch(final InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return super.roles(guildId);
        }
    }
    
    @Nonnull
    @Override
    public NamedCacheView<GuildChannel> channels(final long guildId) {
        final String key = String.format(GUILD_CHANNEL_HASH, guildId);
        final RedisFuture<Map<String, byte[]>> future = redis.hgetall(key);
        try {
            final var channels = new DefaultNamedCacheView<>(GuildChannel::name);
            future.await(5, TimeUnit.SECONDS);
            final Map<String, byte[]> map = future.get();
            map.forEach((id, bytes) -> {
                final var deser = deserialize(bytes, GuildChannel.class);
                if(deser != null) {
                    channels.put(Long.parseUnsignedLong(id), deser);
                }
            });
            return channels;
        } catch(final InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return super.channels(guildId);
        }
    }
    
    @Nonnull
    @Override
    public EntityCache catnip(@Nonnull final Catnip catnip) {
        entityBuilder = new EntityBuilder(catnip);
        return super.catnip(catnip);
    }
    
    private void cache(final String key, final Snowflake entity) {
        final var bytes = serialize(entity);
        redis.hset(key, entity.id(), bytes);
    }
    
    private void cache(final String key, final VoiceState entity) {
        final var bytes = serialize(entity);
        redis.hset(key, entity.userId(), bytes);
    }
    
    private void uncache(final String key, final Snowflake entity) {
        redis.hdel(key, entity.id());
    }
    
    private void uncache(final String key, final VoiceState entity) {
        redis.hdel(key, entity.userId());
    }
    
    private void uncache(final String key, final String id) {
        redis.hdel(key, id);
    }
    
    private <T> CompletableFuture<T> read(final String key, final long id, final Class<T> type) {
        return read(key, Long.toString(id), type);
    }
    
    private <T> CompletableFuture<T> read(final String key, final String id, final Class<T> type) {
        return redis.hget(key, id).thenApply(data -> deserialize(data, type)).toCompletableFuture();
    }
    
    private <T> byte[] serialize(final T obj) {
        try {
            return packer.writeValueAsBytes(obj);
        } catch(final JsonProcessingException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    @Nullable
    private <T> T deserialize(@Nullable final byte[] bytes, @Nonnull final Class<T> type) {
        try {
            if(bytes == null) {
                return null;
            } else {
                return packer.readValue(bytes, type);
            }
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
}
