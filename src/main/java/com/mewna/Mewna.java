package com.mewna;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mewna.data.account.AccountManager;
import com.mewna.api.API;
import com.mewna.catnip.Catnip;
import com.mewna.catnip.CatnipOptions;
import com.mewna.data.Database;
import com.mewna.data.cache.RedisCacheExtension;
import com.mewna.event.SingyeongEventManager;
import com.mewna.plugin.PluginManager;
import com.mewna.plugin.commands.CommandManager;
import com.mewna.plugin.plugins.levels.LevelsImportQueue;
import com.mewna.util.IOUtils;
import com.mewna.util.Profiler;
import com.mewna.util.Ratelimiter;
import com.mewna.util.Translator;
import com.timgroup.statsd.NoOpStatsDClient;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import gg.amy.singyeong.SingyeongClient;
import io.sentry.Sentry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.json.Json;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Optional;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("Singleton")
@Accessors(fluent = true)
public final class Mewna {
    public static final int PRIMARY_COLOUR = 0xdb325c;
    @SuppressWarnings("StaticVariableOfConcreteClass")
    private static final Mewna INSTANCE = new Mewna();
    @Getter
    private final PluginManager pluginManager = new PluginManager(this);
    @Getter
    private final Database database = new Database(this);
    @Getter
    private final Ratelimiter ratelimiter = new Ratelimiter(this);
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private final AccountManager accountManager = new AccountManager(this);
    @Getter
    private final StatsDClient statsClient;
    @Getter
    private final Vertx vertx = Vertx.vertx(new VertxOptions()
            .setAddressResolverOptions(
                    // Work around stupid kubernetes DNS failure modes
                    new AddressResolverOptions().setQueryTimeout(30_000L)));
    @Getter
    private final SingyeongClient singyeong;
    @Getter
    private final int port = Integer.parseInt(Optional.ofNullable(System.getenv("PORT")).orElse("80"));
    @Getter
    private SingyeongEventManager singyeongEventManager;
    @Getter
    private CommandManager commandManager;
    @Getter
    private Catnip catnip;
    @Getter
    private final LevelsImportQueue levelsImportQueue = new LevelsImportQueue(this);
    
    private Mewna() {
        if(System.getenv("STATSD_ENABLED") != null) {
            statsClient = new NonBlockingStatsDClient("v3.backend", System.getenv("STATSD_HOST"), 8125);
        } else {
            statsClient = new NoOpStatsDClient();
        }
        // Initialized here to avoid breaking things
        singyeong = SingyeongClient.create(vertx, System.getenv("SINGYEONG_DSN"), IOUtils.ip() + ':' + port);
    }
    
    public static void main(final String[] args) {
        INSTANCE.start();
    }
    
    public static Mewna getInstance() {
        return INSTANCE;
    }
    
    private void start() {
        final long jvmStart = ManagementFactory.getRuntimeMXBean().getStartTime();
        final long start = System.currentTimeMillis();
        final Profiler profiler = new Profiler("sentry", System.currentTimeMillis());
        logger.info("Starting Mewna backend...");
        Sentry.init();
        profiler.section("jackson");
        // Register jackson modules w/ the v. om instances
        Json.mapper.registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module());
        Json.prettyMapper.registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module());
        // Start loading our data!
        profiler.section("translations");
        Translator.preload();
        commandManager = new CommandManager(this);
        profiler.section("databaseInit");
        database.init();
        levelsImportQueue.start();
        profiler.section("pluginInit");
        pluginManager.init();
        logger.info("Loaded {} commands", commandManager.getCommandMetadata().size());
        // Skip token validation to save on REST reqs
        profiler.section("catnipInit");
        catnip = Catnip.catnip(new CatnipOptions(System.getenv("TOKEN")).validateToken(false), vertx)
                .loadExtension(new RedisCacheExtension(String.format("redis://%s@%s:6379/0",
                        System.getenv("REDIS_PASS"), System.getenv("REDIS_HOST"))))
        ;
        profiler.section("singyeongInit");
        singyeongEventManager = new SingyeongEventManager(this);
        singyeong.connect()
                .thenAccept(__ -> singyeong.onEvent(singyeongEventManager::handle))
                .thenAccept(__ -> singyeong.onInvalid(i -> logger.info("Singyeong invalid: {}: {}", i.nonce(), i.reason())))
                .thenAccept(__ -> new API(this).start())
                .thenAccept(__ -> {
                    long end = System.currentTimeMillis();
                    logger.info("Started in {}ms ({}ms JVM start).", end - start, start - jvmStart);
                    profiler.end();
                    if(System.getenv("DEBUG") != null) {
                        final StringBuilder sb = new StringBuilder();
                        sb.append("[PROFILER]\n");
                        final Optional<Integer> maxLength = profiler.sections().stream()
                                .map(e -> '[' + e.name() + ']')
                                .map(String::length)
                                .max(Integer::compareTo);
                        final int max = maxLength.orElse(0);
                        profiler.sections().forEach(section -> {
                            final String formatted = StringUtils.leftPad('[' + section.name() + ']', max, ' ');
                            sb.append(formatted).append(' ').append(section.end() - section.start()).append("ms\n");
                        });
                        logger.info("Boot profiling:\n{}", sb.toString().trim());
                        final var heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                        final var nonHeap = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
                        final long heapUsed = heap.getUsed() / (1024L * 1024L);
                        final long heapAllocated = heap.getCommitted() / (1024L * 1024L);
                        final long heapTotal = heap.getMax() / (1024L * 1024L);
                        final long heapInit = heap.getInit() / (1024L * 1024L);
                        final long nonHeapUsed = nonHeap.getUsed() / (1024L * 1024L);
                        final long nonHeapAllocated = nonHeap.getCommitted() / (1024L * 1024L);
                        final long nonHeapTotal = nonHeap.getMax() / (1024L * 1024L);
                        final long nonHeapInit = nonHeap.getInit() / (1024L * 1024L);
                        
                        final var out = "[HEAP]\n" +
                                "     [Init] " + heapInit + "MB\n" +
                                "     [Used] " + heapUsed + "MB\n" +
                                "    [Alloc] " + heapAllocated + "MB\n" +
                                "    [Total] " + heapTotal + "MB\n" +
                                "[NONHEAP]\n" +
                                "     [Init] " + nonHeapInit + "MB\n" +
                                "     [Used] " + nonHeapUsed + "MB\n" +
                                "    [Alloc] " + nonHeapAllocated + "MB\n" +
                                "    [Total] " + nonHeapTotal + "MB\n";
                        logger.info("Boot RAM:\n{}", out);
                    }
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        levelsImportQueue.setRun(false);
                        // TODO: singyeong disconnect??
                    }));
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }
}
