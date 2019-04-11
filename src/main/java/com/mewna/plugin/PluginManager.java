package com.mewna.plugin;

import com.google.common.collect.ImmutableMap;
import com.mewna.Mewna;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.commands.Command;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.util.CurrencyHelper;
import com.mewna.util.UserAgentInterceptor;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.sentry.Sentry;
import lombok.Getter;
import lombok.Value;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

import static com.mewna.util.Async.move;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("WeakerAccess")
public class PluginManager {
    private final Collection<Class<?>> loaded = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Class<?>, Function<Class<?>, ?>> injectionClasses;
    @Getter
    private final Mewna mewna;
    @Getter
    private final CurrencyHelper currencyHelper;
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            // Lie about our user agent so we can ex. grab discord profile images
            .addInterceptor(new UserAgentInterceptor("Mozilla/5.0 (X11; Linux x86_64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/71.0.3578.98 " +
                    "Safari/537.36"))
            .build();
    private final Map<Class<?>, ? super Object> pluginMap = new HashMap<>();
    @Getter
    private final Collection<PluginMetadata> pluginMetadata = new ArrayList<>();
    @Getter
    private final Collection<Class<? extends PluginSettings>> settingsClasses = new ArrayList<>();
    
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
     */
    private final Map<String, HashSet<EventHolder>> discordEventHandlers = new HashMap<>();
    
    public PluginManager(final Mewna mewna) {
        this.mewna = mewna;
        currencyHelper = new CurrencyHelper();
        
        injectionClasses = ImmutableMap.<Class<?>, Function<Class<?>, ?>>builder()
                .put(Mewna.class, __ -> this.mewna)
                .put(Logger.class, LoggerFactory::getLogger)
                .put(Database.class, __ -> this.mewna.database())
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
                            logger.error("Couldn't inject {}#{}: {}", obj.getClass().getName(), f.getName(), e);
                            Sentry.capture(e);
                        }
                    }
                }
            }
            injectable = injectable.getSuperclass();
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
                if(!pluginAnnotation.enabled()) {
                    logger.warn("Not loading disabled plugin {}: {}", pluginAnnotation.name(), pluginAnnotation.desc());
                    return;
                }
                logger.info("Loading plugin {}: {}", pluginAnnotation.name(), pluginAnnotation.desc());
                final Object pluginInstance = c.getDeclaredConstructor().newInstance();
                inject(pluginInstance);
                
                for(final Method m : c.getDeclaredMethods()) {
                    m.setAccessible(true);
                    if(m.isAnnotationPresent(Command.class)) {
                        mewna.commandManager().loadCommandsFromMethod(pluginInstance, pluginAnnotation, c, m);
                    } else if(m.isAnnotationPresent(Event.class)) {
                        loadEventHandler(pluginInstance, m);
                    }
                }
                
                pluginMap.put(c, pluginInstance);
                if(pluginAnnotation.enabled() && !pluginAnnotation.owner() && !pluginAnnotation.staff()) {
                    pluginMetadata.add(new PluginMetadata(pluginAnnotation.name(), pluginAnnotation.desc(), c, pluginAnnotation.settings()));
                    settingsClasses.add(pluginAnnotation.settings());
                }
                logger.info("Finished loading plugin {}: {}", pluginAnnotation.name(), pluginAnnotation.desc());
            } catch(final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                Sentry.capture(e);
                e.printStackTrace();
            }
        }
    }
    
    public <T> void processEvent(final String type, final T event) {
        move(() -> Optional.ofNullable(discordEventHandlers.get(type)).ifPresentOrElse(x -> x.forEach(h -> {
            try {
                h.getHandle().invoke(h.getHolder(), event);
            } catch(final Exception e) {
                Sentry.capture(e);
            }
        }), () -> Sentry.capture("No handler for event type: " + type)));
    }
    
    @SuppressWarnings({"unused", "unchecked"})
    public <T> T getPlugin(final Class<T> cls) {
        return (T) pluginMap.get(cls);
    }
    
    @Value
    public static final class EventHolder {
        private String eventType;
        private Method handle;
        private Object holder;
    }
    
    @Value
    public static final class PluginMetadata {
        private String name;
        private String desc;
        private Class<?> pluginClass;
        private Class<? extends PluginSettings> settingsClass;
    }
}
