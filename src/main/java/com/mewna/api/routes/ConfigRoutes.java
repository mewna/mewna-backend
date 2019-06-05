package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.data.plugin.PluginSettings;
import com.mewna.plugin.plugins.settings.SecretSettings;
import io.sentry.Sentry;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.mewna.util.Async.move;
import static com.mewna.util.MewnaFutures.block;

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
        router.get("/v3/guild/:id/config").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            final List<? extends PluginSettings> settings = mewna.pluginManager().getSettingsClasses()
                    .stream()
                    .map(cls -> block(mewna.database().getOrBaseSettings(cls, id)))
                    .collect(Collectors.toList());
            if(settings.isEmpty()) {
                ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("invalid settings"))).encode());
                return;
            }
            final JsonObject out = new JsonObject();
            settings.stream().filter(s -> !(s instanceof SecretSettings)).forEach(s -> {
                if(s == null) {
                    ctx.response().end("{}");
                    throw new IllegalStateException();
                }
                final String name = keyFromValue(mewna.database().getPluginSettingsByName(), s.getClass());
                out.put(Objects.requireNonNull(name), JsonObject.mapFrom(s));
            });
            ctx.response().end(out.encode());
        }));
        
        router.post("/v3/guild/:id/config").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            
            final var data = ctx.getBody().toJsonObject();
            final Collection<Boolean> updates = new ArrayList<>();
            data.getMap().keySet().forEach(key -> {
                final var update = data.getJsonObject(key);
                final var settings = block(mewna.database().getOrBaseSettings(key, id));
                final boolean validate = settings.validate(update);
                if(validate) {
                    try {
                        updates.add(settings.updateSettings(mewna.database(), update));
                    } catch(final Exception e) {
                        Sentry.capture(e);
                        updates.add(false);
                    }
                } else {
                    updates.add(false);
                }
            });
            
            if(updates.stream().allMatch(e -> e)) {
                ctx.response().end(data.encode());
            } else {
                ctx.response().end(new JsonObject().put("error", new JsonArray().add("invalid settings")).encode());
            }
        }));
    }
}
