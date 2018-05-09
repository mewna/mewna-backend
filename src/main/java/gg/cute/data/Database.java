package gg.cute.data;

import gg.amy.pgorm.PgStore;
import gg.cute.Cute;
import gg.cute.cache.entity.Guild;
import gg.cute.cache.entity.User;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

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
    private final Cute cute;
    @Getter
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private boolean init;
    @Getter
    private PgStore store;
    
    private JedisPool jedisPool;
    
    public Database(final Cute cute) {
        this.cute = cute;
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
        
        premap(GuildSettings.class, Player.class);
    }
    
    private void premap(final Class<?>... clz) {
        for(final Class<?> c : clz) {
            logger.info("Premapping class: " + c.getName());
            store.mapSync(c);
        }
    }
    
    public GuildSettings getGuildSettings(final Guild src) {
        return getGuildSettings(src.getId());
    }
    
    public GuildSettings getGuildSettings(final String id) {
        return store.mapSync(GuildSettings.class).load(id).orElse(GuildSettings.base(id));
    }
    
    public void saveGuildSettings(final GuildSettings settings) {
        store.mapSync(GuildSettings.class).save(settings);
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
        try(Jedis jedis = jedisPool.getResource()) {
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
