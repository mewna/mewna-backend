package com.mewna.data;

import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.accounts.timeline.TimelinePost;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.util.SafeVertxCompletableFuture;
import com.mewna.data.posts.Post;
import com.mewna.plugin.Plugin;
import com.mewna.util.Profiler;
import gg.amy.pgorm.PgStore;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.sentry.Sentry;
import io.vertx.core.json.JsonObject;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Database-level abstraction
 *
 * @author amy
 * @since 4/14/18.
 */
@SuppressWarnings({"unused", "WeakerAccess", "SqlResolve"})
public class Database {
    @Getter
    private final Mewna mewna;
    @Getter
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private final Map<String, Class<? extends PluginSettings>> pluginSettingsByName = new HashMap<>();
    private final OkHttpClient client = new OkHttpClient();
    private boolean init;
    @Getter
    private PgStore store;
    private JedisPool jedisPool;
    
    public Database(final Mewna mewna) {
        this.mewna = mewna;
    }
    
    public void init() {
        if(init) {
            return;
        }
        init = true;
        logger.info("Connecting to Redis...");
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(10);
        config.setMaxTotal(10);
        config.setMaxWaitMillis(500);
        jedisPool = new JedisPool(config, System.getenv("REDIS_HOST"));
        logger.info("Redis connection pool ready!");
        
        store = PgStore.fromEnv();
        store.connect();
        
        mapSettingsClasses();
        
        premap(Player.class, Account.class, Server.class, TimelinePost.class);
        
        // Webhooks table is created manually, because it doesn't need to be JSON:b:
        store.sql("CREATE TABLE IF NOT EXISTS discord_webhooks (channel TEXT PRIMARY KEY NOT NULL UNIQUE, guild TEXT NOT NULL, " +
                "id TEXT NOT NULL, secret TEXT NOT NULL)");
        store.sql("CREATE INDEX IF NOT EXISTS idx_discord_webhooks_guilds ON discord_webhooks (guild);");
        store.sql("CREATE INDEX IF NOT EXISTS idx_discord_webhooks_ids ON discord_webhooks (id);");
    }
    
    //////////////
    // Internal //
    //////////////
    
    @SuppressWarnings("unchecked")
    private void mapSettingsClasses() {
        final List<Class<?>> classes = new ArrayList<>();
        new FastClasspathScanner(Plugin.class.getPackage().getName()).matchAllStandardClasses(cls -> {
            if(PluginSettings.class.isAssignableFrom(cls) && !cls.equals(PluginSettings.class)) {
                classes.add(cls);
                pluginSettingsByName.put(cls.getSimpleName().toLowerCase().replace("settings", ""),
                        (Class<? extends PluginSettings>) cls);
            }
        }).scan();
        premap(classes.toArray(new Class[0]));
    }
    
    private void premap(final Class<?>... clz) {
        for(final Class<?> c : clz) {
            logger.info("Premapping class: " + c.getName());
            store.mapSync(c);
            store.mapAsync(c);
        }
    }
    
    ///////////
    // Redis //
    ///////////
    
    public void redis(final Consumer<Jedis> c) {
        try(final Jedis jedis = jedisPool.getResource()) {
            jedis.auth(System.getenv("REDIS_PASS"));
            c.accept(jedis);
        }
    }
    
    public void tredis(final Consumer<Transaction> t) {
        redis(c -> {
            final Transaction transaction = c.multi();
            t.accept(transaction);
            transaction.exec();
        });
    }
    
    public <T> void cache(final String type, final String key, final T value) {
        redis(r -> r.set("mewna:cache:" + type + ':' + key, JsonObject.mapFrom(value).encode()));
    }
    
    public <T> T cacheRead(final String type, final String key, final Class<T> cls) {
        final Object[] cached = {null};
        redis(r -> {
            final String json = r.get("mewna:cache:" + type + ':' + key);
            if(json != null) {
                cached[0] = new JsonObject(json).mapTo(cls);
            }
        });
        //noinspection unchecked
        return (T) cached[0];
    }
    
    public void cachePrune(final String type, final String key) {
        redis(r -> r.del("mewna:cache:" + type + ':' + key));
    }
    
    public CompletableFuture<Boolean> lock(final String key) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        tryLock(key, future, 0);
        return future;
    }
    
    private void tryLock(final String key, final CompletableFuture<Boolean> future, final int tries) {
        // I really don't know???
        // Somehow it's deadlocking???
        final int timeout = 5;
        final int maxTries = 10;
        if(tries > maxTries) {
            future.complete(false);
        }
        redis(c -> {
            final String set = c.set(key, "value", "NX", "EX", timeout);
            if("OK".equalsIgnoreCase(set)) {
                future.complete(true);
            } else {
                mewna.catnip().vertx().setTimer(100, __ -> tryLock(key, future, tries + 1));
            }
        });
    }
    
    public void unlock(final String key) {
        redis(c -> c.del(key));
    }
    
    public CompletableFuture<Boolean> lockPlayer(final Player player) {
        return lockPlayer(player.getId());
    }
    
    public CompletableFuture<Boolean> lockPlayer(final String id) {
        mewna.statsClient().increment("playerLocksTaken", 1);
        return lock("mewna:locks:players:" + id);
    }
    
    public void unlockPlayer(final Player player) {
        unlockPlayer(player.getId());
    }
    
    public void unlockPlayer(final String id) {
        mewna.statsClient().increment("playerLocksReleased", 1);
        unlock("mewna:locks:players:" + id);
    }
    
    //////////////
    // Webhooks //
    //////////////
    
    public Optional<Webhook> getWebhook(final String channelId) {
        final OptionalHolder<Webhook> holder = new OptionalHolder<>();
        store.sql("SELECT * FROM discord_webhooks WHERE channel = ?;", p -> {
            p.setString(1, channelId);
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                resultSet.next();
                final String channel = resultSet.getString("channel");
                final String guild = resultSet.getString("guild");
                final String id = resultSet.getString("id");
                final String secret = resultSet.getString("secret");
                holder.setValue(new Webhook(channel, guild, id, secret));
            }
        });
        return holder.value;
    }
    
    public Optional<Webhook> getWebhookById(final String hookId) {
        final OptionalHolder<Webhook> holder = new OptionalHolder<>();
        store.sql("SELECT * FROM discord_webhooks WHERE id = ?;", p -> {
            p.setString(1, hookId);
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                resultSet.next();
                final String channel = resultSet.getString("channel");
                final String guild = resultSet.getString("guild");
                final String id = resultSet.getString("id");
                final String secret = resultSet.getString("secret");
                holder.setValue(new Webhook(channel, guild, id, secret));
            }
        });
        return holder.value;
    }
    
    public void addWebhook(final Webhook webhook) {
        final Optional<Webhook> hook = getWebhook(webhook.getChannel());
        //noinspection StatementWithEmptyBody
        if(hook.isEmpty()) {
            store.sql("INSERT INTO discord_webhooks (channel, guild, id, secret) VALUES (?, ?, ?, ?);", p -> {
                p.setString(1, webhook.getChannel());
                p.setString(2, webhook.getGuild());
                p.setString(3, webhook.getId());
                p.setString(4, webhook.getSecret());
                p.execute();
            });
        } else {
            // TODO: Just delete it?
        }
    }
    
    public List<Webhook> getAllWebhooks(final String guildId) {
        final List<Webhook> webhooks = new ArrayList<>();
        store.sql("SELECT * FROM discord_webhooks WHERE guild = ?;", p -> {
            p.setString(1, guildId);
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    final String channel = resultSet.getString("channel");
                    final String guild = resultSet.getString("guild");
                    final String id = resultSet.getString("id");
                    final String secret = resultSet.getString("secret");
                    webhooks.add(new Webhook(channel, guild, id, secret));
                }
            }
        });
        return webhooks;
    }
    
    public void deleteWebhook(final String channel) {
        // TODO: Twitch settings need to be checked / updated
        store.sql("DELETE FROM discord_webhooks WHERE channel = ?;", p -> {
            p.setString(1, channel);
            p.execute();
        });
    }
    
    public void deleteWebhookById(final String id) {
        // TODO: Twitch settings need to be checked / updated
        final Optional<Webhook> webhookById = getWebhookById(id);
        if(webhookById.isPresent()) {
            final Webhook webhook = webhookById.get();
            store.sql("DELETE FROM discord_webhooks WHERE id = ?;", p -> {
                p.setString(1, id);
                p.execute();
            });
            try {
                //noinspection UnnecessarilyQualifiedInnerClassAccess
                client.newCall(new Request.Builder()
                        .delete()
                        .url("https://discordapp.com/api/v6/webhooks/" + webhook.getId() + '/' + webhook.getSecret())
                        .build()).execute().close();
            } catch(final IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    //////////////
    // Settings //
    //////////////
    
    @SuppressWarnings("unchecked")
    public <T extends PluginSettings> Class<T> getSettingsClassByType(final String type) {
        return (Class<T>) pluginSettingsByName.get(type);
    }
    
    private <T extends PluginSettings> CompletableFuture<Optional<T>> getSettingsByType(final Class<T> type, final String id) {
        return store.mapAsync(type).load(id);
    }
    
    public <T extends PluginSettings> CompletableFuture<T> getOrBaseSettings(final String type, final String id) {
        final Class<T> cls = getSettingsClassByType(type);
        if(cls == null) {
            throw new IllegalArgumentException("Type '" + type + "' not a valid settingClass.");
        }
        return getOrBaseSettings(cls, id);
    }
    
    @SuppressWarnings({"unchecked", "OptionalAssignedToNull"})
    public <T extends PluginSettings> CompletableFuture<T> getOrBaseSettings(final Class<T> type, final String id) {
        if(!store.isMappedSync(type) && !store.isMappedAsync(type)) {
            throw new IllegalArgumentException("Attempted to get settings of type " + type.getName() + ", but it's not mapped!");
        }
        final T cached = cacheRead("settings:" + type.getSimpleName(), id, type);
        if(cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return getSettingsByType(type, id)
                .exceptionally(e -> {
                    Sentry.capture(e);
                    return null;
                })
                .thenApply(maybeSettings -> {
                    // This is a valid thing to do - a null value is returned
                    // when the settings *are not present due to an error*, ie
                    // in an exceptional case that generally shouldn't ever happen
                    // Thanks, java.util.Optional, for being so trash.
                    if(maybeSettings == null) {
                        // future.fail("Failing due to database issues");
                        throw new IllegalStateException("Failing due to database issues");
                    } else if(maybeSettings.isPresent()) {
                        final T maybe = maybeSettings.get();
                        // We're joining inside of NON-vx threads here, so I'm pretty sure this is safe?
                        // Honestly I just couldn't think of a better way to do it......
                        final T settings = (T) maybe.refreshCommands().otherRefresh();
                        saveSettings(settings);
                        // Cache 'em
                        cache("settings:" + type.getSimpleName(), id, settings);
                        return settings;
                    } else {
                        try {
                            final T base = type.getConstructor(String.class).newInstance(id);
                            saveSettings(base);
                            cache("settings:" + type.getSimpleName(), id, base);
                            return base;
                        } catch(final IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                            Sentry.capture(e);
                            throw new RuntimeException(e);
                        }
                    }
                });
    }
    
    @SuppressWarnings("unchecked")
    public <T extends PluginSettings> void saveSettings(final T settings) {
        cachePrune("settings:" + settings.getClass().getSimpleName(), settings.getId());
        // This is technically valid
        store.mapSync((Class<T>) settings.getClass()).save(settings);
    }
    
    /////////////
    // Players //
    /////////////
    
    public CompletableFuture<Optional<Player>> getOptionalPlayer(final String id, final Profiler profiler) {
        if(profiler != null) {
            profiler.section("playerMapAsync");
        }
        final Player cached = cacheRead("player", id, Player.class);
        if(cached != null) {
            return SafeVertxCompletableFuture.completedFuture(Optional.of(cached));
        }
        return store.mapAsync(Player.class).load(id)
                .thenApply(player -> {
                    cache("player", id, player);
                    return player;
                })
                .exceptionally(e -> {
                    logger.info("Error fetching player {}", id, e);
                    Sentry.capture(e);
                    // This is okay I promise ;-;
                    //noinspection OptionalAssignedToNull
                    return null;
                });
    }
    
    public CompletableFuture<Player> getPlayer(final User user, final Profiler profiler) {
        return getOptionalPlayer(user.id(), profiler)
                .thenApply(o -> {
                    if(profiler != null) {
                        profiler.section("playerValidation");
                    }
                    // This is fine!
                    // o is null only in case of database errors
                    //noinspection OptionalAssignedToNull
                    if(o == null) {
                        throw new IllegalStateException("Database error fetching player " + user.id());
                    } else {
                        return o.orElseGet(() -> {
                            if(profiler != null) {
                                profiler.section("playerRecreate");
                            }
                            final Player base = Player.base(user.id());
                            savePlayer(base);
                            // If we don't have a player, then we also need to create an account for them
                            if(mewna.accountManager().getAccountByLinkedDiscord(user.id()).isEmpty()) {
                                mewna.accountManager().createNewDiscordLinkedAccount(base, user);
                            }
                            return base;
                        });
                    }
                })
                .exceptionally(e -> {
                    Sentry.capture(e);
                    return null;
                });
    }
    
    public CompletableFuture<Void> savePlayer(final Player player) {
        player.cleanup();
        return lockPlayer(player)
                .thenApply(state -> {
                    if(state) {
                        return (Void) null;
                    } else {
                        throw new IllegalStateException("Tried to save player " + player.getId() + ", but couldn't lock!");
                    }
                }).thenCompose(__ -> store.mapAsync(Player.class).save(player)
                        .thenAccept(___ -> {
                            cachePrune("player", player.getId());
                            unlockPlayer(player);
                        }));
    }
    
    //////////////
    // Accounts //
    //////////////
    
    public Optional<Account> getAccountById(final String id) {
        final Account cached = cacheRead("account", id, Account.class);
        if(cached != null) {
            return Optional.of(cached);
        }
        final Optional<Account> loaded = store.mapSync(Account.class).load(id);
        
        loaded.ifPresent(account -> cache("account", id, account));
        return loaded;
    }
    
    public Optional<Account> getAccountByDiscordId(final String id) {
        final Account cached = cacheRead("account", id, Account.class);
        if(cached != null) {
            return Optional.of(cached);
        }
        
        final OptionalHolder<Account> holder = new OptionalHolder<>();
        
        store.sql("SELECT data FROM " + store.mapSync(Account.class).getTableName() + " WHERE data->>'discordAccountId' = ?;", p -> {
            p.setString(1, id);
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                resultSet.next();
                final String data = resultSet.getString("data");
                try {
                    holder.setValue(new JsonObject(data).mapTo(Account.class));
                } catch(final Exception e) {
                    Sentry.capture(e);
                    throw new RuntimeException(e);
                }
            }
        });
        
        holder.value.ifPresent(account -> cache("account", id, account));
        
        return holder.value;
    }
    
    public void saveAccount(final Account account) {
        cachePrune("account", account.id());
        store.mapSync(Account.class).save(account);
    }
    
    ////////////////////
    // Timeline posts //
    ////////////////////
    
    public Optional<TimelinePost> getTimelinePost(final String id) {
        final TimelinePost post = cacheRead("post", id, TimelinePost.class);
        if(post != null) {
            return Optional.of(post);
        }
        final Optional<TimelinePost> optional = store.mapSync(TimelinePost.class).load(id);
        optional.ifPresent(timelinePost -> cache("post", timelinePost.getId(), timelinePost));
        return optional;
    }
    
    public boolean saveTimelinePost(final TimelinePost post) {
        if(!post.isSystem()) {
            final Post body = post.getContent().getBody();
            if(!body.validate()) {
                return false;
            }
        }
        cachePrune("post", post.getId());
        store.mapSync(TimelinePost.class).save(post);
        return true;
    }
    
    public boolean deleteTimelinePost(final String id) {
        final Optional<TimelinePost> timelinePost = getTimelinePost(id);
        if(timelinePost.isEmpty()) {
            return false;
        }
        if(timelinePost.get().isSystem()) {
            return false;
        }
        cachePrune("post", id);
        store.sql("DELETE FROM " + store.mapSync(TimelinePost.class).getTableName() + " WHERE id = ?;", q -> {
            q.setString(1, id);
            q.execute();
        });
        return true;
    }
    
    public boolean updateTimelinePost(final TimelinePost post) {
        final Optional<TimelinePost> maybeTimelinePost = getTimelinePost(post.getId());
        if(maybeTimelinePost.isEmpty()) {
            return false;
        }
        final TimelinePost timelinePost = maybeTimelinePost.get();
        if(timelinePost.isSystem()) {
            return false;
        }
        if(post.isSystem() != timelinePost.isSystem()) {
            return false;
        }
        if(!post.isSystem()) {
            final Post body = post.getContent().getBody();
            if(!body.validate()) {
                return false;
            }
            post.getContent().getBody().setBoops(timelinePost.getContent().getBody().getBoops());
            post.setAuthor(timelinePost.getAuthor());
        }
        cachePrune("post", post.getId());
        store.mapSync(TimelinePost.class).save(post);
        return true;
    }
    
    public List<TimelinePost> getTimelinePostChunk(final String id, final long limit, final long after) {
        // TODO: How the tato can this be cached???
        final List<TimelinePost> posts = new ArrayList<>();
        store.sql("SELECT data FROM " + store.mapSync(TimelinePost.class).getTableName()
                + " WHERE data->>'author' = ? AND id::bigint > ? ORDER BY id::bigint DESC LIMIT " + limit + ';', q -> {
            q.setString(1, id);
            q.setLong(2, after);
            final ResultSet resultSet = q.executeQuery();
            while(resultSet.next()) {
                final String data = resultSet.getString("data");
                posts.add(new JsonObject(data).mapTo(TimelinePost.class));
            }
        });
        return posts;
    }
    
    public List<TimelinePost> getLast100TimelinePosts(final String id) {
        final List<TimelinePost> posts = new ArrayList<>();
        store.sql("SELECT data FROM " + store.mapSync(TimelinePost.class).getTableName()
                + " WHERE data->>'author' = ? ORDER BY id::bigint DESC LIMIT 100;", q -> {
            q.setString(1, id);
            final ResultSet resultSet = q.executeQuery();
            while(resultSet.next()) {
                final String data = resultSet.getString("data");
                posts.add(new JsonObject(data).mapTo(TimelinePost.class));
            }
        });
        
        return posts;
    }
    
    public List<TimelinePost> getAllTimelinePosts(final String id) {
        final List<TimelinePost> posts = new ArrayList<>();
        store.sql("SELECT data FROM " + store.mapSync(TimelinePost.class).getTableName() + " WHERE data->>'author' = ? ORDER BY id::bigint DESC;", q -> {
            q.setString(1, id);
            final ResultSet resultSet = q.executeQuery();
            while(resultSet.next()) {
                final String data = resultSet.getString("data");
                posts.add(new JsonObject(data).mapTo(TimelinePost.class));
            }
        });
        
        return posts;
    }
    
    //////////////////////////
    // Server page settings //
    //////////////////////////
    
    public Server getServer(final String id) {
        final Server server = cacheRead("server", id, Server.class);
        if(server != null) {
            return server;
        }
        final Optional<Server> maybeServer = store.mapSync(Server.class).load(id);
        if(maybeServer.isPresent()) {
            return maybeServer.get();
        } else {
            final Server base = new Server(id);
            store.mapSync(Server.class).save(base);
            cache("server", id, base);
            return base;
        }
    }
    
    public void saveServer(final Server server) {
        store.mapSync(Server.class).save(server);
        cachePrune("server", server.getId());
    }
    
    //////////
    // Misc //
    //////////
    
    public String language(final String guild) {
        // TODO: Make this return a real locale value...
        return "en_US";
    }
    
    private final class OptionalHolder<T> {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<T> value;
        
        private OptionalHolder() {
            value = Optional.empty();
        }
        
        private void setValue(final T data) {
            value = Optional.ofNullable(data);
        }
    }
}
