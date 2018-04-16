package gg.cute.plugin;

import com.google.common.collect.ImmutableMap;
import gg.cute.Cute;
import gg.cute.cache.entity.Channel;
import gg.cute.cache.entity.User;
import gg.cute.data.Database;
import gg.cute.data.DiscordSettings;
import gg.cute.plugin.metadata.Payment;
import gg.cute.plugin.metadata.Ratelimit;
import gg.cute.plugin.util.CurrencyHelper;
import gg.cute.util.Time;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import lombok.Getter;
import lombok.Value;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("WeakerAccess")
public class PluginManager {
    private static final List<String> PREFIXES;
    
    static {
        PREFIXES = Arrays.asList(Optional.ofNullable(System.getenv("PREFIXES")).orElse("bmy!,b:,=").split(","));
    }
    
    private final Map<String, CommandWrapper> commands = new HashMap<>();
    private final Collection<Class<?>> loaded = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Class<?>, Function<Class<?>, Object>> injectionClasses;
    @Getter
    private final Cute cute;
    private final CurrencyHelper currencyHelper;
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
    
    public PluginManager(final Cute cute) {
        this.cute = cute;
        currencyHelper = new CurrencyHelper();
        
        injectionClasses = ImmutableMap.<Class<?>, Function<Class<?>, Object>>builder()
                .put(Cute.class, __ -> this.cute)
                .put(Logger.class, LoggerFactory::getLogger)
                .put(Database.class, __ -> this.cute.getDatabase())
                .put(Random.class, __ -> new Random())
                .put(CurrencyHelper.class, __ -> currencyHelper)
                .put(OkHttpClient.class, __ -> okHttpClient)
                .build();
    }
    
    public void init() {
        inject(currencyHelper);
        new FastClasspathScanner(Plugin.class.getPackage().getName())
                .matchClassesWithAnnotation(Plugin.class, this::initPlugin).scan();
    }
    
    private <T> void inject(final T obj) {
        Class<?> injectable = obj.getClass();
        
        while(injectable != null && !Objects.equals(injectable, Object.class)) {
            for(final Field f : injectable.getDeclaredFields()) {
                if(injectionClasses.containsKey(f.getType())) {
                    f.setAccessible(true);
                    try {
                        f.set(obj, injectionClasses.get(f.getType()).apply(obj.getClass()));
                        logger.debug("Injected into {}#{}", obj.getClass().getName(), f.getName());
                    } catch(final IllegalAccessException e) {
                        logger.error("Coouldn't inject {}#{}: {}", obj.getClass().getName(), f.getName(), e);
                    }
                }
            }
            injectable = injectable.getSuperclass();
        }
    }
    
    private void initPlugin(final Class<?> c) {
        if(loaded.contains(c)) {
            return;
        }
        loaded.add(c);
        if(c.isAnnotationPresent(Plugin.class)) {
            try {
                final Plugin p = c.getDeclaredAnnotation(Plugin.class);
                final Object plugin = c.newInstance();
                inject(plugin);
                
                for(final Method m : c.getDeclaredMethods()) {
                    m.setAccessible(true);
                    if(m.isAnnotationPresent(Command.class)) {
                        if(m.getParameterCount() != 1) {
                            logger.warn("@Command method '{}' doesn't take a single parameter!", m.getName());
                            continue;
                        }
                        if(!m.getParameterTypes()[0].equals(CommandContext.class)) {
                            logger.warn("@Command method '{}' doesn't take a single CommandContext parameter!", m.getName());
                            continue;
                        }
                        final Command cmd = m.getDeclaredAnnotation(Command.class);
                        
                        for(final String s : cmd.names()) {
                            commands.put(s.toLowerCase(), new CommandWrapper(s.toLowerCase(),
                                    cmd.aliased() ? cmd.names()[0].toLowerCase() : s.toLowerCase(), cmd.names(),
                                    m.getDeclaredAnnotation(Ratelimit.class),
                                    m.getDeclaredAnnotation(Payment.class),
                                    cmd.desc(),
                                    cmd.owner(), plugin, m));
                            logger.info("Loaded plugin command '{}' for plugin '{}' ({})", s,
                                    p.value(), c.getName());
                        }
                    }
                }
            } catch(final InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    @SuppressWarnings("TypeMayBeWeakened")
    private List<String> getAllPrefixes(final DiscordSettings guildSettings) {
        final List<String> prefixes = new ArrayList<>(PREFIXES);
        final String custom = guildSettings.getCustomPrefix();
        if(custom != null && !custom.isEmpty()) {
            prefixes.add(custom);
        }
        return prefixes;
    }
    
    public void handleMessage(final JSONObject data) {
        try {
            // TODO: Temporary dev. block
            final String channelId = data.getString("channel_id");
            final String guildId = data.getString("guild_id");
            final User user = cute.getCache().getUser(data.getJSONObject("author").getString("id"));
            if(user == null) {
                logger.error("Got message from unknown (uncached) user {}!?", data.getJSONObject("author").getString("id"));
                return;
            }
            if(!user.getId().equals("128316294742147072")) {
                return;
            }
            
            if(user.isBot()) {
                return;
            }
            // Parse prefixes
            String content = data.getString("content");
            String prefix = null;
            boolean found = false;
            final DiscordSettings settings = cute.getDatabase().getGuildSettings(guildId);
            for(final String p : getAllPrefixes(settings)) {
                if(p != null && !p.isEmpty()) {
                    if(content.toLowerCase().startsWith(p.toLowerCase())) {
                        prefix = p;
                        found = true;
                    }
                }
            }
            if(found) {
                content = content.substring(prefix.length()).trim();
                if(content.isEmpty()) {
                    return;
                }
                final String[] split = content.split("\\s+", 2);
                final String commandName = split[0];
                final String argstr = split.length > 1 ? split[1] : "";
                final ArrayList<String> args = new ArrayList<>(Arrays.asList(argstr.split("\\s+")));
                if(commands.containsKey(commandName)) {
                    final CommandWrapper cmd = commands.get(commandName);
                    if(cmd.isOwner() && !user.getId().equalsIgnoreCase("128316294742147072")) {
                        return;
                    }
                    
                    if(cmd.getRatelimit() != null) {
                        final String baseName = cmd.getBaseName();
                        
                        final String ratelimitKey;
                        switch(cmd.getRatelimit().type()) {
                            case GUILD:
                                ratelimitKey = guildId;
                                break;
                            case GLOBAL:
                                ratelimitKey = user.getId();
                                break;
                            case CHANNEL:
                                ratelimitKey = channelId;
                                break;
                            default:
                                ratelimitKey = user.getId();
                                break;
                        }
                        
                        final ImmutablePair<Boolean, Long> check = cute.getRatelimiter().checkUpdateRatelimit(user.getId(),
                                baseName + ':' + ratelimitKey,
                                TimeUnit.SECONDS.toMillis(cmd.getRatelimit().time()));
                        if(check.left) {
                            cute.getRestJDA().sendMessage(channelId,
                                    String.format("You're using that command too fast! Try again in **%s**.",
                                            Time.toHumanReadableDuration(check.right))).queue();
                            return;
                        }
                    }
    
                    final Channel channel = cute.getCache().getChannel(channelId);
                    final List<User> mentions = new ArrayList<>();
                    for(final Object o : data.getJSONArray("mentions")) {
                        final JSONObject j = (JSONObject) o;
                        // TODO: Build this from the JSON object instead of hitting the cache all the time?
                        mentions.add(cute.getCache().getUser(j.getString("id")));
                    }
                    
                    long cost = 0L;
                    // Commands may require payment before running
                    if(cmd.getPayment() != null) {
                        // By default, try to make the minimum payment
                        String maybePayment = cmd.getPayment().min() + "";
                        if(cmd.getPayment().fromFirstArg()) {
                            // If we calculate the payment from the first argument:
                            // - if there is no argument, go with the minimum payment
                            // - if there is an argument, try to parse it
                            if(args.isEmpty()) {
                                maybePayment = cmd.getPayment().min() + "";
                            } else {
                                maybePayment = args.get(0);
                            }
                        }
    
                        final CommandContext paymentCtx = new CommandContext(user, commandName, args, argstr,
                                cute.getCache().getGuild(channel.getGuildId()), channel, mentions, settings,
                                cute.getDatabase().getPlayer(user), 0L);
                        
                        final ImmutablePair<Boolean, Long> res = currencyHelper.handlePayment(paymentCtx,
                                maybePayment, cmd.getPayment().min(),
                                cmd.getPayment().max());
                        // If we can make the payment, set the cost and continue
                        // Otherwise, return early (the payment-handler sends error messages for us)
                        if(res.left) {
                            cost = res.right;
                        } else {
                            return;
                        }
                    }
                    
                    final CommandContext ctx = new CommandContext(user, commandName,
                            args, argstr,
                            cute.getCache().getGuild(channel.getGuildId()), channel, mentions, settings,
                            cute.getDatabase().getPlayer(user), cost);
                    
                    try {
                        cmd.getMethod().invoke(cmd.getPlugin(), ctx);
                    } catch(final IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch(final Throwable t) {
            logger.error("Error at high-level command processor:", t);
        }
    }
    
    @Value
    public static final class CommandWrapper {
        private String name;
        private String baseName;
        private String[] names;
        private Ratelimit ratelimit;
        private Payment payment;
        private String desc;
        private boolean owner;
        private Object plugin;
        private Method method;
    }
}
