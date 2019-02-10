package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.plugin.plugins.levels.LevelsImporter;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

/**
 * @author amy
 * @since 2/10/19.
 */
public class ImportRoutes implements RouteGroup {
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.post("/data/levels/import/:id/mee6").handler(ctx -> {
            final String id = ctx.request().getParam("id");
            // This makes me feel better about passing untrusted user input from the url parameters
            if(!id.matches("\\d{17,20}")) {
                ctx.response().end(new JsonObject().encode());
                return;
            }
            LevelsImporter.importMEE6Levels(id);
            ctx.response().end(new JsonObject().encode());
        });
    }
}
