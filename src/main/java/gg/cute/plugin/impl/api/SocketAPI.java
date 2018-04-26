package gg.cute.plugin.impl.api;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author amy
 * @since 4/25/18.
 */
@WebSocket
public class SocketAPI {
    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @OnWebSocketConnect
    public void connected(final Session session) {
        sessions.add(session);
        logger.info("Session connected from " + session.getRemote().getInetSocketAddress().getAddress().getHostAddress());
    }
    
    @OnWebSocketClose
    public void closed(final Session session, final int statusCode, final String reason) {
        sessions.remove(session);
        logger.info("Session closed from " + session.getRemote().getInetSocketAddress().getAddress().getHostAddress());
    }
    
    @OnWebSocketMessage
    public void message(final Session session, final String message) throws IOException {
        System.out.println("Got: " + message);   // Print message
        session.getRemote().sendString(message); // and send it back
    }
}
