package com.mewna;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mewna.accounts.AccountManager;
import com.mewna.catnip.Catnip;
import com.mewna.catnip.CatnipOptions;
import com.mewna.data.Database;
import com.mewna.data.DiscordCache;
import com.mewna.event.SingyeongEventManager;
import com.mewna.paypal.PaypalHandler;
import com.mewna.plugin.CommandManager;
import com.mewna.plugin.PluginManager;
import com.mewna.plugin.util.TextureManager;
import com.mewna.util.Ratelimiter;
import com.mewna.util.Translator;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import gg.amy.singyeong.SingyeongClient;
import gg.amy.singyeong.SingyeongType;
import io.sentry.Sentry;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("Singleton")
@Accessors(fluent = true)
public final class Mewna {
    public static final String APP_ID = "mewna-backend";
    @SuppressWarnings("StaticVariableOfConcreteClass")
    private static final Mewna INSTANCE = new Mewna();
    
    @Getter
    private SingyeongEventManager singyeongEventManager;
    @Getter
    private final PluginManager pluginManager = new PluginManager(this);
    @Getter
    private final CommandManager commandManager = new CommandManager(this);
    @Getter
    private final Database database = new Database(this);
    @Getter
    private final Ratelimiter ratelimiter = new Ratelimiter(this);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Getter
    private final AccountManager accountManager = new AccountManager(this);
    
    @Getter
    private final PaypalHandler paypalHandler = new PaypalHandler(this);
    
    @Getter
    private final StatsDClient statsClient;
    
    @Getter
    private final Vertx vertx = Vertx.vertx();
    @Getter
    private final SingyeongClient singyeong = SingyeongClient.create(vertx, System.getenv("SINGYEONG_DSN"));
    @Getter
    private Catnip catnip;
    
    private Mewna() {
        if(System.getenv("STATSD_ENABLED") != null) {
            statsClient = new NonBlockingStatsDClient("v2.backend", System.getenv("STATSD_HOST"), 8125);
        } else {
            statsClient = new NoOpStatsDClient();
        }
    }
    
    public static void main(final String[] args) {
        INSTANCE.start();
    }
    
    public static Mewna getInstance() {
        return INSTANCE;
    }
    
    private void start() {
        logger.info("Starting Mewna backend...");
        Sentry.init();
        // Register jackson modules w/ the v. om instances
        Json.mapper.registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module());
        Json.prettyMapper.registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module());
        // Start loading our data!
        Translator.preload();
        TextureManager.preload(this);
        database.init();
        pluginManager.init();
        catnip = Catnip.catnip(new CatnipOptions(System.getenv("TOKEN")), vertx);
        singyeongEventManager = new SingyeongEventManager(this);
        singyeong.connect()
                .thenAccept(__ -> singyeong.updateMetadata("backend-key", SingyeongType.STRING, "mewna-backend"))
                .thenAccept(__ -> DiscordCache.setup())
                .thenAccept(__ -> singyeong.onEvent(singyeongEventManager::handle))
                .thenAccept(__ -> singyeong.onInvalid(i -> logger.info("Singyeong invalid: {}: {}", i.nonce(), i.reason())))
                .thenAccept(__ -> new API(this).start())
                .thenAccept(__ -> logger.info("Finished starting!"))
        .exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }
}
