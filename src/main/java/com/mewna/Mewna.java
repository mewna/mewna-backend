package com.mewna;

import com.mewna.accounts.AccountManager;
import com.mewna.cache.DiscordCache;
import com.mewna.data.Database;
import com.mewna.event.EventManager;
import com.mewna.jda.RestJDA;
import com.mewna.queue.Q;
import com.mewna.paypal.PaypalHandler;
import com.mewna.plugin.CommandManager;
import com.mewna.plugin.PluginManager;
import com.mewna.plugin.util.TextureManager;
import com.mewna.util.Ratelimiter;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import io.sentry.Sentry;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("Singleton")
public final class Mewna {
    @SuppressWarnings("StaticVariableOfConcreteClass")
    private static final Mewna INSTANCE = new Mewna();
    
    @Getter
    private final EventManager eventManager = new EventManager(this);
    @Getter
    private final PluginManager pluginManager = new PluginManager(this);
    @Getter
    private final CommandManager commandManager = new CommandManager(this);
    @Getter
    private final RestJDA restJDA = new RestJDA(System.getenv("TOKEN"));
    @Getter
    private final Database database = new Database(this);
    @Getter
    private final Ratelimiter ratelimiter = new Ratelimiter(this);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private Q nats;
    
    @Getter
    private final AccountManager accountManager = new AccountManager(this);
    
    @Getter
    private final PaypalHandler paypalHandler = new PaypalHandler(this);
    
    @Getter
    private final StatsDClient statsClient;
    
    private Mewna() {
        if(System.getenv("STATSD_ENABLED") != null) {
            statsClient = new NonBlockingStatsDClient(null, System.getenv("STATSD_HOST"), 8125);
        } else {
            statsClient = new NoOpStatsDClient();
        }
    }
    
    public static void main(final String[] args) {
        INSTANCE.start();
    }
    
    @SuppressWarnings("unused")
    public static Mewna getInstance() {
        return INSTANCE;
    }
    
    private void start() {
        logger.info("Starting Mewna backend...");
        Sentry.init();
        TextureManager.preload(this);
        eventManager.getCache().connect();
        database.init();
        pluginManager.init();
        new API(this).start();
        nats = new Q(this);
        nats.connect();
        logger.info("Finished starting!");
    }
    
    @SuppressWarnings("WeakerAccess")
    public DiscordCache getCache() {
        return eventManager.getCache();
    }
}
