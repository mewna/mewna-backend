package gg.cute;

import gg.cute.cache.DiscordCache;
import gg.cute.event.EventHandler;
import gg.cute.jda.RestJDA;
import gg.cute.nats.NatsServer;
import gg.cute.plugin.PluginManager;
import lombok.Getter;

/**
 * @author amy
 * @since 4/8/18.
 */
public class Cute {
    @Getter
    private NatsServer nats;
    
    @Getter
    private EventHandler eventHandler = new EventHandler(this);
    
    @Getter
    private final PluginManager pluginManager = new PluginManager(this);
    
    @Getter
    private final RestJDA restJDA = new RestJDA(System.getenv("TOKEN"));
    
    private Cute() {
    }
    
    public static void main(String[] args) {
        new Cute().start();
    }
    
    private void start() {
        eventHandler.getCache().connect();
        pluginManager.init();
        nats = new NatsServer(this);
        nats.connect();
    }
    
    public DiscordCache getCache() {
        return eventHandler.getCache();
    }
}
