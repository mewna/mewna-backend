package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.stream.Collectors;

/**
 * @author amy
 * @since 2/10/19.
 */
public class MetadataRoutes implements RouteGroup {
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.get("/data/commands/metadata").handler(ctx -> {
            final JsonArray arr = new JsonArray(mewna.commandManager().getCommandMetadata()
                    .stream()
                    .map(JsonObject::mapFrom)
                    .collect(Collectors.toList()));
            ctx.response().putHeader("Content-Type", "application/json")
                    .end(arr.encode());
        });
        router.get("/data/plugins/metadata").handler(ctx -> {
            final JsonArray data = new JsonArray(mewna.pluginManager().getPluginMetadata().stream()
                    .map(JsonObject::mapFrom)
                    .collect(Collectors.toList()));
            data.forEach(e -> {
                // ;-;
                // TODO: Find better solution
                ((JsonObject) e).remove("settingsClass");
                ((JsonObject) e).remove("pluginClass");
            });
            ctx.response().putHeader("Content-Type", "application/json").end(data.encode());
        });
    }
}
