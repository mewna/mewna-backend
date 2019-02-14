package com.mewna.api.routes.v3;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.data.PluginSettings;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import static com.mewna.util.Async.move;

/**
 * @author amy
 * @since 2/14/19.
 */
public class ConfigRoutes implements RouteGroup {
    private <K, V> K keyFromValue(final Map<K, V> source, final V value) {
        for(final Entry<K, V> e : source.entrySet()) {
            if(e.getValue().equals(value)) {
                return e.getKey();
            }
        }
        return null;
    }
    
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.get("/v3/config/guild/:id").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final List<CompletableFuture<? extends PluginSettings>> futures = new ArrayList<>();
            mewna.pluginManager().getSettingsClasses().forEach(cls -> futures.add(mewna.database().getOrBaseSettings(cls, id)));
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(__ -> {
                final JsonObject out = new JsonObject();
                futures.forEach(f -> {
                    final PluginSettings settings = f.getNow(null);
                    if(settings == null) {
                        ctx.response().end("{}");
                        throw new IllegalStateException();
                    }
                    final String name = keyFromValue(mewna.database().getPluginSettingsByName(), settings.getClass());
                    //noinspection ConstantConditions
                    out.put(name, JsonObject.mapFrom(settings));
                });
                ctx.response().end(out.encode());
            });
        }));
    }
}
