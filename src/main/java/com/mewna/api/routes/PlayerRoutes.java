package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.data.Player;
import com.mewna.plugin.plugins.PluginEconomy;
import io.sentry.Sentry;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.mewna.util.Async.move;
import static com.mewna.util.Translator.$;

/**
 * @author amy
 * @since 2/10/19.
 */
public class PlayerRoutes implements RouteGroup {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.get("/data/player/:id").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
    
            mewna.database().getOptionalPlayer(id)
                    .thenAccept(o -> ctx.response().putHeader("Content-Type", "application/json")
                            .end(o.map(JsonObject::mapFrom)
                                    .orElse(new JsonObject())
                                    .encode()));
        }));
    
        router.post("/data/votes/dbl").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final JsonObject body = ctx.getBodyAsJson();
            @SuppressWarnings("unused")
            final String bot = body.getString("bot");
            final String user = body.getString("user");
            final String type = body.getString("type");
            final boolean isWeekend = body.getBoolean("isWeekend");

            mewna.database().getOptionalPlayer(user).thenAccept(player -> {
                if(player.isPresent()) {
                    final int amount = PluginEconomy.VOTE_BONUS * (isWeekend ? 2 : 1);
                    final Player p = player.get();
                    switch(type.toLowerCase()) {
                        case "upvote":
                        case "vote": {
                            p.setBalance(p.getBalance() + amount);
                            mewna.database().savePlayer(p);
                            mewna.statsClient().increment("votes.dbl", 1);
                            final String message;
                            if(isWeekend) {
                                mewna.statsClient().increment("votes.dbl.weekend", 1);
                                message = $("en_US", "votes.dbl.weekend").replace("$amount", amount + "");
                            } else {
                                message = $("en_US", "votes.dbl.normal").replace("$amount", amount + "");
                            }
                            mewna.catnip().rest().user().createDM(user).thenAccept(channel -> channel.sendMessage(message))
                                    .thenAccept(__ -> logger.info("Sent upvote DM to {}", user))
                                    .exceptionally(e -> {
                                        Sentry.capture(e);
                                        return null;
                                    })
                            ;
                            break;
                        }
                        case "test": {
                            mewna.catnip().rest().user().createDM(user)
                                    .thenAccept(channel -> channel.sendMessage("```Javascript\n" + body.encodePrettily() + "\n```"));
                            break;
                        }
                    }
                }
            });
            ctx.response().end("{}");
        }));
    }
}
