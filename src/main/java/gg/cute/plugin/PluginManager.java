package gg.cute.plugin;

import com.google.common.collect.ImmutableMap;
import gg.cute.Cute;
import gg.cute.cache.entity.Channel;
import gg.cute.cache.entity.User;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import lombok.Getter;
import lombok.Value;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("WeakerAccess")
public class PluginManager {
    private static final List<String> PREFIXES;
    
    static {
        PREFIXES = Arrays.asList(Optional.ofNullable(System.getenv("PREFIXES")).orElse("bmy!,b:").split(","));
    }
    
    private final Map<String, CommandWrapper> commands = new HashMap<>();
    private final Collection<Class<?>> loaded = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Class<?>, Function<Class<?>, Object>> injectionClasses;
    @Getter
    private final Cute cute;
    
    public PluginManager(final Cute cute) {
        this.cute = cute;
        
        injectionClasses = ImmutableMap.<Class<?>, Function<Class<?>, Object>>builder()
                .put(Cute.class, __ -> this.cute)
                .put(Logger.class, LoggerFactory::getLogger)
                .build();
    }
    
    public void init() {
        new FastClasspathScanner(Plugin.class.getPackage().getName())
                .matchClassesWithAnnotation(Plugin.class, this::initPlugin).scan();
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
                // Inject
                Class<?> injectable = c;
                
                while(injectable != null && !Objects.equals(injectable, Object.class)) {
                    for(final Field f : injectable.getDeclaredFields()) {
                        if(injectionClasses.containsKey(f.getType())) {
                            f.setAccessible(true);
                            f.set(plugin, injectionClasses.get(f.getType()).apply(plugin.getClass()));
                            logger.debug("Injected into {}#{}", plugin.getClass().getName(), f.getName());
                        }
                    }
                    injectable = injectable.getSuperclass();
                }
                
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
                            commands.put(s.toLowerCase(), new CommandWrapper(s.toLowerCase(), cmd.names(), cmd.desc(),
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
    
    public void handleMessage(final JSONObject data) {
        // Parse prefixes
        // TODO: Per-guild support?
        String content = data.getString("content");
        String prefix = null;
        boolean found = false;
        for(final String p : PREFIXES) {
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
            if(commands.containsKey(commandName)) {
                final Channel channel = cute.getCache().getChannel(data.getString("channel_id"));
                final List<User> mentions = new ArrayList<>();
                for(final Object o : data.getJSONArray("mentions")) {
                    final JSONObject j = (JSONObject) o;
                    // TODO: Build this from the object instead of hitting the cache all the time?
                    mentions.add(cute.getCache().getUser(j.getString("id")));
                }
                final CommandContext ctx = new CommandContext(
                        cute.getCache().getUser(data.getJSONObject("author").getString("id")),
                        commandName,
                        new ArrayList<>(Arrays.asList(argstr.split("\\s+"))),
                        argstr,
                        cute.getCache().getGuild(channel.getGuildId()),
                        channel,
                        mentions
                );
                final CommandWrapper cmd = commands.get(commandName);
                try {
                    cmd.getMethod().invoke(cmd.getPlugin(), ctx);
                } catch(final IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    @Value
    public static final class CommandWrapper {
        private String name;
        private String[] names;
        private String desc;
        private boolean owner;
        private Object plugin;
        private Method method;
    }
}
