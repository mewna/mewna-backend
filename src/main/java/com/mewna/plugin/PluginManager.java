package com.mewna.plugin;

import com.google.common.collect.ImmutableMap;
import com.mewna.Mewna;
import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.data.Database;
import com.mewna.data.GuildSettings;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.message.MessageCreateEvent;
import com.mewna.plugin.metadata.Payment;
import com.mewna.plugin.metadata.Ratelimit;
import com.mewna.plugin.util.CurrencyHelper;
import com.mewna.util.Time;
import com.mewna.util.UserAgentInterceptor;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import lombok.Getter;
import lombok.Value;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
@SuppressWarnings({"WeakerAccess", "OverlyCoupledClass"})
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
    private final Mewna mewna;
    private final CurrencyHelper currencyHelper;
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(new UserAgentInterceptor("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.117 Safari/537.36"))
            .build();
    @Getter
    private final Collection<CommandMetadata> commandMetadata = new ArrayList<>();
    
    /**
     * Event handlers for Discord events. Things like cache are done before the
     * handlers here are hit by any events, but beyond that handful of things,
     * just about anything goes. <p />
     * <p>
     * The reason for having this is because of the functionality needed.
     * Before, there was a event handler that just passed some MESSAGE_CREATE
     * events on to this class, but the problem then comes handling other
     * events nicely. This is solved by just allowing plugins to register
     * handlers for these events. <p />
     * <p>
     * Note that we don't have to worry about thread-safety (per se) on this
     * field since it's only ever written once at boot. <p />
     * <p>
     * TODO: Consider going as far as making command-handling itself a plugin?
     */
    private final Map<String, HashSet<EventHolder>> discordEventHandlers = new HashMap<>();
    
    public PluginManager(final Mewna mewna) {
        this.mewna = mewna;
        currencyHelper = new CurrencyHelper();
        
        injectionClasses = ImmutableMap.<Class<?>, Function<Class<?>, Object>>builder()
                .put(Mewna.class, __ -> this.mewna)
                .put(Logger.class, LoggerFactory::getLogger)
                .put(Database.class, __ -> this.mewna.getDatabase())
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
                if(f.isAnnotationPresent(Inject.class)) {
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
            }
            injectable = injectable.getSuperclass();
        }
    }
    
    private void loadCommandsFromMethod(final Object pluginInstance, final Plugin pluginAnnotation,
                                        final Class<?> pluginClass, final Method m) {
        if(m.getParameterCount() != 1) {
            logger.warn("@Command method '{}' doesn't take a single parameter!", m.getName());
            return;
        }
        if(!m.getParameterTypes()[0].equals(CommandContext.class)) {
            logger.warn("@Command method '{}' doesn't take a single CommandContext parameter!", m.getName());
            return;
        }
        final Command cmd = m.getDeclaredAnnotation(Command.class);
        // Load commands
        for(final String s : cmd.names()) {
            commands.put(s.toLowerCase(), new CommandWrapper(s.toLowerCase(),
                    cmd.aliased() ? cmd.names()[0].toLowerCase() : s.toLowerCase(), cmd.names(),
                    m.getDeclaredAnnotation(Ratelimit.class),
                    m.getDeclaredAnnotation(Payment.class),
                    cmd.desc(),
                    cmd.owner(), pluginInstance, m));
            logger.info("Loaded plugin command '{}' for plugin '{}' ({})", s,
                    pluginAnnotation.name(), pluginClass.getName());
        }
        // Load metadata
        if(cmd.aliased()) {
            final List<String> tmp = new ArrayList<>(Arrays.asList(cmd.names()));
            final String name = tmp.remove(0);
            final String[] aliases = tmp.toArray(new String[0]);
            commandMetadata.add(new CommandMetadata(name, cmd.desc(), aliases, cmd.usage(), cmd.examples()));
        } else {
            for(final String name : cmd.names()) {
                commandMetadata.add(new CommandMetadata(name, cmd.desc(), new String[0], cmd.usage(), cmd.examples()));
            }
        }
    }
    
    private void loadEventHandler(final Object plugin, final Method m) {
        // Map event handlers to handle various Discord events or w/e
        final Event event = m.getDeclaredAnnotation(Event.class);
        if(!discordEventHandlers.containsKey(event.value())) {
            discordEventHandlers.put(event.value(), new HashSet<>());
        }
        discordEventHandlers.get(event.value()).add(new EventHolder(event.value(), m, plugin));
    }
    
    private void initPlugin(final Class<?> c) {
        if(loaded.contains(c)) {
            return;
        }
        loaded.add(c);
        if(c.isAnnotationPresent(Plugin.class)) {
            try {
                final Plugin pluginAnnotation = c.getDeclaredAnnotation(Plugin.class);
                logger.info("Loading plugin {}: {}", pluginAnnotation.name(), pluginAnnotation.desc());
                final Object pluginInstance = c.newInstance();
                inject(pluginInstance);
                
                for(final Method m : c.getDeclaredMethods()) {
                    m.setAccessible(true);
                    if(m.isAnnotationPresent(Command.class)) {
                        loadCommandsFromMethod(pluginInstance, pluginAnnotation, c, m);
                    } else if(m.isAnnotationPresent(Event.class)) {
                        loadEventHandler(pluginInstance, m);
                    }
                }
                logger.info("Finished loading plugin {}: {}", pluginAnnotation.name(), pluginAnnotation.desc());
            } catch(final InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    
    @SuppressWarnings("TypeMayBeWeakened")
    private List<String> getAllPrefixes(final GuildSettings guildSettings) {
        final List<String> prefixes = new ArrayList<>(PREFIXES);
        final String custom = guildSettings.getCustomPrefix();
        if(custom != null && !custom.isEmpty()) {
            prefixes.add(custom);
        }
        return prefixes;
    }
    
    public void tryExecCommand(final JSONObject data) {
        try {
            final String channelId = data.getString("channel_id");
            // See https://github.com/discordapp/discord-api-docs/issues/582
            // Note this isn't in the docs yet, but should be at some point (hopefully soon)
            final String guildId = data.getString("guild_id");
            final User user = mewna.getCache().getUser(data.getJSONObject("author").getString("id"));
            if(user == null) {
                logger.error("Got message from unknown (uncached) user {}!?", data.getJSONObject("author").getString("id"));
                return;
            }
            // TODO: Temporary dev. block
            if(!user.getId().equals("128316294742147072")) {
                return;
            }
            
            if(user.isBot()) {
                return;
            }
            // TODO: Need a check to block commands in DMs.
            
            // Collect cache data
            final Guild guild = mewna.getCache().getGuild(guildId);
            final Channel channel = mewna.getCache().getChannel(channelId);
            final List<User> mentions = new ArrayList<>();
            for(final Object o : data.getJSONArray("mentions")) {
                final JSONObject j = (JSONObject) o;
                // TODO: Build this from the JSON object instead of hitting the cache all the time?
                mentions.add(mewna.getCache().getUser(j.getString("id")));
            }
            
            // Parse prefixes
            String content = data.getString("content");
            String prefix = null;
            boolean found = false;
            final GuildSettings settings = mewna.getDatabase().getGuildSettings(guildId);
            for(final String p : getAllPrefixes(settings)) {
                if(p != null && !p.isEmpty()) {
                    if(content.toLowerCase().startsWith(p.toLowerCase())) {
                        prefix = p;
                        found = true;
                    }
                }
            }
            // TODO: There's gotta be a way to refactor this out into smaller methods...
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
                        
                        final ImmutablePair<Boolean, Long> check = mewna.getRatelimiter().checkUpdateRatelimit(user.getId(),
                                baseName + ':' + ratelimitKey,
                                TimeUnit.SECONDS.toMillis(cmd.getRatelimit().time()));
                        if(check.left) {
                            mewna.getRestJDA().sendMessage(channelId,
                                    String.format("You're using that command too fast! Try again in **%s**.",
                                            Time.toHumanReadableDuration(check.right))).queue();
                            return;
                        }
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
                                guild, channel, mentions, settings,
                                mewna.getDatabase().getPlayer(user), 0L, prefix);
                        
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
                            guild, channel, mentions, settings,
                            mewna.getDatabase().getPlayer(user), cost, prefix);
                    
                    try {
                        cmd.getMethod().invoke(cmd.getPlugin(), ctx);
                    } catch(final IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // No prefix found, pass it down as an event
                processEvent("MESSAGE_CREATE", new MessageCreateEvent(user, channel, guild, mentions,
                        data.getString("content"), data.getBoolean("mention_everyone")));
            }
        } catch(final Throwable t) {
            logger.error("Error at high-level command processor:", t);
        }
    }
    
    public <T extends BaseEvent> void processEvent(final String type, final T event) {
        Optional.ofNullable(discordEventHandlers.get(type)).ifPresent(x -> x.forEach(h -> {
            try {
                h.getHandle().invoke(h.getHolder(), event);
            } catch(final IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }));
    }
    
    @Value
    public static final class EventHolder {
        private String eventType;
        private Method handle;
        private Object holder;
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
    
    @Value
    public static final class CommandMetadata {
        private String name;
        private String desc;
        private String[] aliases;
        private String[] usage;
        private String[] examples;
    }
}
