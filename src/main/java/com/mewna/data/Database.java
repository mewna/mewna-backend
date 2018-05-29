package com.mewna.data;

import com.mewna.Mewna;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
        
        premap(Player.class);
    }
    
    private void mapSettingsClasses() {
        final List<Class<?>> classes = new ArrayList<>();
        new FastClasspathScanner(Plugin.class.getPackage().getName()).matchAllStandardClasses(cls -> {
            if(PluginSettings.class.isAssignableFrom(cls) && !cls.equals(PluginSettings.class)) {
                boolean hasBase = false;
                for(final Method method : cls.getMethods()) {
                    if(// @formatter:off
                            // Verify name
                            method.getName().equals("base")
                            // Verify static
                            && Modifier.isStatic(method.getModifiers())
                            // Verify params
                            && method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(String.class)
                        ) {
                        // @formatter:on
                        hasBase = true;
                        break;
                    }
                }
                if(hasBase) {
                    classes.add(cls);
                    //noinspection unchecked
                    pluginSettingsByName.put(cls.getSimpleName().toLowerCase().replace("settings", ""),
                            (Class<? extends PluginSettings>) cls);
                } else {
                    logger.error("Was asked to map settings class {}, but it has no base()?!", cls.getName());
                }
            }
        }).scan();
        premap(classes.toArray(new Class[0]));
    }
    
    @SuppressWarnings("unchecked")
    public <T extends PluginSettings> Class<T> getSettingsClassByType(final String type) {
        return (Class<T>) pluginSettingsByName.get(type);
    }
    
    private void premap(final Class<?>... clz) {
        for(final Class<?> c : clz) {
            logger.info("Premapping class: " + c.getName());
            store.mapSync(c);
        }
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
            // Base the settings and return
            try {
                // TODO: There *must* be a better way to express this...
                @SuppressWarnings("unchecked")
                final T base = (T) type.getMethod("base", String.class).invoke(null, id);
                saveSettings(base);
                return base;
            } catch(final IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
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
    
    public Player getPlayer(final String id) {
        return store.mapSync(Player.class).load(id).orElse(Player.base(id));
    }
    
    public void savePlayer(final Player player) {
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
}
