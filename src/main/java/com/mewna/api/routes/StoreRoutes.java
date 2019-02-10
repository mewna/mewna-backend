package com.mewna.api.routes;

import com.google.common.collect.ImmutableList;
import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.plugin.util.TextureManager;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import static com.mewna.util.Async.move;

/**
 * @author amy
 * @since 2/10/19.
 */
public class StoreRoutes implements RouteGroup {
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.get("/data/backgrounds/packs").handler(ctx -> ctx.response().end(JsonObject.mapFrom(TextureManager.getPacks()).encode()));
        router.post("/data/store/checkout/start").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final JsonObject body = ctx.getBodyAsJson();
            ctx.response().putHeader("Content-Type", "application/json")
                    .end(mewna.paypalHandler().startPayment(
                            body.getString("userId"),
                            body.getString("sku")
                    ).encode());
        }));
        router.post("/data/store/checkout/confirm").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final JsonObject body = ctx.getBodyAsJson();
            ctx.response().putHeader("Content-Type", "application/json")
                    .end(mewna.paypalHandler().finishPayment(
                            body.getString("userId"),
                            body.getString("paymentId"),
                            body.getString("payerId")
                    ).encode());
        }));
        router.get("/data/store/manifest").handler(ctx -> ctx.response().putHeader("Content-Type", "application/json")
                .end(new JsonArray(ImmutableList.copyOf(mewna.paypalHandler().getSkus())).encode()));
    }
}
