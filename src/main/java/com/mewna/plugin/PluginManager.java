package com.mewna.plugin;

import com.google.common.collect.ImmutableMap;
import com.mewna.Mewna;
import com.mewna.data.Database;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.util.CurrencyHelper;
import com.mewna.util.UserAgentInterceptor;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import lombok.AccessLevel;
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

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings({"WeakerAccess", "OverlyCoupledClass"})
public class PluginManager {
    private final Collection<Class<?>> loaded = new HashSet<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Class<?>, Function<Class<?>, Object>> injectionClasses;
    @Getter
    private final Mewna mewna;
    @Getter(AccessLevel.PACKAGE)
    private final CurrencyHelper currencyHelper;
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(new UserAgentInterceptor("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.117 Safari/537.36"))
            .build();
    
    @Getter
    private final Collection<PluginMetadata> pluginMetadata = new ArrayList<>();
    
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
                        mewna.getCommandManager().loadCommandsFromMethod(pluginInstance, pluginAnnotation, c, m);
                    } else if(m.isAnnotationPresent(Event.class)) {
                        loadEventHandler(pluginInstance, m);
                    }
                }
                
                if(pluginAnnotation.enabled()) {
                    pluginMetadata.add(new PluginMetadata(pluginAnnotation.name(), pluginAnnotation.desc()));
                }
                logger.info("Finished loading plugin {}: {}", pluginAnnotation.name(), pluginAnnotation.desc());
            } catch(final InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
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
    public static final class PluginMetadata {
        private String name;
        private String desc;
    }
}
