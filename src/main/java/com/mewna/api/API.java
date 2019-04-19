package com.mewna.api;

import com.google.common.collect.ImmutableList;
import com.mewna.Mewna;
import com.mewna.api.routes.v3.*;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author amy
 * @since 6/10/18.
 */
@RequiredArgsConstructor
public class API {
    private final Mewna mewna;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<RouteGroup> routes = ImmutableList.of(
            // v3
            new ConfigRoutes(),
            new GuildRoutes(),
            new CacheRoutes(),
            new PostRoutes(),
            new UserRoutes()
    );
    
    public void start() {
        logger.info("Starting API server...");
        final HttpServer server = mewna.vertx().createHttpServer();
        final Router router = Router.router(mewna.vertx());
        routes.forEach(e -> {
            logger.info("Registering routes from RouteGroup: {}", e);
            e.registerRoutes(mewna, router);
        });
        server.requestHandler(router).listen(mewna.port());
    }
}
