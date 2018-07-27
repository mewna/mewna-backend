package com.mewna.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.accounts.timeline.TimelinePost;
import com.mewna.cache.entity.User;
import com.mewna.plugin.Plugin;
import gg.amy.pgorm.PgStore;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import lombok.Getter;
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
import java.util.function.Consumer;

/**
 * Database-level abstraction
 *
 * @author amy
 * @since 4/14/18.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Database {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    @Getter
    private final Mewna mewna;
    @Getter
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, Class<? extends PluginSettings>> pluginSettingsByName = new HashMap<>();
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
        config.setMaxIdle(1500);
        config.setMaxTotal(1500);
        config.setMaxWaitMillis(500);
        jedisPool = new JedisPool(config, System.getenv("REDIS_HOST"));
        logger.info("Redis connection pool ready!");
        
        store = PgStore.fromEnv();
        store.connect();
        
        mapSettingsClasses();
        
        premap(Player.class, Account.class, TimelinePost.class);
        
        // Webhooks table is created manually, because it doesn't need to be JSON:b:
        store.sql("CREATE TABLE IF NOT EXISTS discord_webhooks (channel TEXT PRIMARY KEY NOT NULL UNIQUE, guild TEXT NOT NULL, " +
                "id TEXT NOT NULL, secret TEXT NOT NULL)");
        store.sql("CREATE INDEX IF NOT EXISTS idx_discord_webhooks_guilds ON discord_webhooks (guild);");
        store.sql("CREATE INDEX IF NOT EXISTS idx_discord_webhooks_ids ON discord_webhooks (id);");
    }
    
    //////////////
    // Internal //
    //////////////
    
    private void mapSettingsClasses() {
        final List<Class<?>> classes = new ArrayList<>();
        new FastClasspathScanner(Plugin.class.getPackage().getName()).matchAllStandardClasses(cls -> {
            if(PluginSettings.class.isAssignableFrom(cls) && !cls.equals(PluginSettings.class)) {
                classes.add(cls);
                //noinspection unchecked
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
        }
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
    
    public void addWebhook(final Webhook webhook) {
        store.sql("INSERT INTO discord_webhooks (channel, guild, id, secret) VALUES (?, ?, ?, ?);", p -> {
            p.setString(1, webhook.getChannel());
            p.setString(2, webhook.getGuild());
            p.setString(3, webhook.getId());
            p.setString(4, webhook.getSecret());
            p.execute();
        });
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
        store.sql("DELETE FROM discord_webhooks WHERE channel = ?;", p -> {
            p.setString(1, channel);
            p.execute();
        });
    }
    
    //////////////
    // Settings //
    //////////////
    
    @SuppressWarnings("unchecked")
    public <T extends PluginSettings> Class<T> getSettingsClassByType(final String type) {
        return (Class<T>) pluginSettingsByName.get(type);
    }
    
    private <T extends PluginSettings> Optional<T> getSettingsByType(final Class<T> type, final String id) {
        return store.mapSync(type).load(id);
    }
    
    public <T extends PluginSettings> T getOrBaseSettings(final String type, final String id) {
        final Class<T> cls = getSettingsClassByType(type);
        if(cls == null) {
            throw new IllegalArgumentException("Type '" + type + "' not a valid settingClass.");
        }
        return getOrBaseSettings(cls, id);
    }
    
    public <T extends PluginSettings> T getOrBaseSettings(final Class<T> type, final String id) {
        if(!store.isMappedSync(type) && !store.isMappedAsync(type)) {
            throw new IllegalArgumentException("Attempted to get settings of type " + type.getName() + ", but it's not mapped!");
        }
        final Optional<T> maybeSettings = getSettingsByType(type, id);
        if(maybeSettings.isPresent()) {
            return maybeSettings.get();
        } else {
            try {
                final T base = type.getConstructor(String.class).newInstance(id);
                saveSettings(base);
                return base;
            } catch(final IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public <T extends PluginSettings> void saveSettings(final T settings) {
        // This is technically valid
        //noinspection unchecked
        store.mapSync((Class<T>) settings.getClass()).save(settings);
    }
    
    public Player getPlayer(final User src) {
        return getPlayer(src.getId());
    }
    
    /////////////
    // Players //
    /////////////
    
    /**
     * {@link #getPlayer(String)} will create a new player if one does not
     * exist. This is not always the correct thing to do, so we give a way to
     * just directly get an {@code Optional<Player>} instead of creating a new
     * one and returning that. This method is mainly useful for read-only
     * operations.
     *
     * @param id The player id to search for
     *
     * @return An {@link Optional} that might contain a {@link Player}.
     */
    public Optional<Player> getOptionalPlayer(final String id) {
        return store.mapSync(Player.class).load(id);
    }
    
    public Player getPlayer(final String id) {
        return getOptionalPlayer(id).orElseGet(() -> {
            final Player base = Player.base(id);
            savePlayer(base);
            // If we don't have a player, then we also need to create an account for them
            if(!mewna.getAccountManager().getAccountByLinkedDiscord(id).isPresent()) {
                final User user = mewna.getCache().getUser(id);
                if(user != null) {
                    mewna.getAccountManager().createNewDiscordLinkedAccount(base, user);
                } else {
                    throw new IllegalStateException("Couldn't create account for " + id + ": Missing in cache!?");
                }
            }
            return base;
        });
    }
    
    public void savePlayer(final Player player) {
        player.cleanup();
        store.mapSync(Player.class).save(player);
    }
    
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
    
    public Optional<Account> getAccountById(final String id) {
        return store.mapSync(Account.class).load(id);
    }
    
    //////////////
    // Accounts //
    //////////////
    
    public Optional<Account> getAccountByDiscordId(final String id) {
        final OptionalHolder<Account> holder = new OptionalHolder<>();
        
        store.sql("SELECT data FROM " + store.mapSync(Account.class).getTableName() + " WHERE data->>'discordAccountId' = ?;", p -> {
            p.setString(1, id);
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                resultSet.next();
                final String data = resultSet.getString("data");
                try {
                    holder.setValue(MAPPER.readValue(data, Account.class));
                } catch(final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        
        return holder.value;
    }
    
    public void saveAccount(final Account account) {
        store.mapSync(Account.class).save(account);
    }
    
    public void savePost(final TimelinePost post) {
        store.mapSync(TimelinePost.class).save(post);
    }
    
    public List<TimelinePost> getLast100TimelinePosts(final String id) {
        final List<TimelinePost> posts = new ArrayList<>();
        store.sql("SELECT data FROM " + store.mapSync(TimelinePost.class).getTableName() + " WHERE data->>'author' = ? ORDER BY id DESC LIMIT 100;", q -> {
            q.setString(1, id);
            final ResultSet resultSet = q.executeQuery();
            while(resultSet.next()) {
                final String data = resultSet.getString("data");
                try {
                    posts.add(MAPPER.readValue(data, TimelinePost.class));
                } catch(final IOException e) {
                    e.printStackTrace();
                }
            }
        });
        
        return posts;
    }
    
    public List<TimelinePost> getAllTimelinePosts(final String id) {
        final List<TimelinePost> posts = new ArrayList<>();
        store.sql("SELECT data FROM " + store.mapSync(TimelinePost.class).getTableName() + " WHERE data->>'author' = ? ORDER BY id DESC;", q -> {
            q.setString(1, id);
            final ResultSet resultSet = q.executeQuery();
            while(resultSet.next()) {
                final String data = resultSet.getString("data");
                try {
                    posts.add(MAPPER.readValue(data, TimelinePost.class));
                } catch(final IOException e) {
                    e.printStackTrace();
                }
            }
        });
        
        return posts;
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
