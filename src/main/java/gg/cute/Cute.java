package gg.cute;

import gg.cute.event.EventHandler;
import gg.cute.nats.NatsServer;
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
    
    private Cute() {
    
    }
    
    public static void main(String[] args) {
        new Cute().start();
    }
    
    private void start() {
        eventHandler.getCache().connect();
        nats = new NatsServer(this);
        nats.connect();
    }
}
