package com.mewna.api;

import com.mewna.Mewna;
import io.vertx.ext.web.Router;

/**
 * @author amy
 * @since 2/10/19.
 */
public interface RouteGroup {
    void registerRoutes(Mewna mewna, Router router);
}
