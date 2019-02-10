package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.api.RouteGroup;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.Optional;
import java.util.stream.Collectors;

import static com.mewna.util.Async.move;

/**
 * @author amy
 * @since 2/10/19.
 */
public class AccountRoutes implements RouteGroup {
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.get("/data/account/:id").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            ctx.response().putHeader("Content-Type", "application/json")
                    .end(JsonObject.mapFrom(mewna.database().getAccountById(id)).encode());
        }));
        router.get("/data/account/:id/links").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            ctx.response().putHeader("Content-Type", "application/json");
            final Optional<Account> maybeAccount = mewna.accountManager().getAccountById(id);
            if(maybeAccount.isPresent()) {
                final Account account = maybeAccount.get();
                final JsonObject data = new JsonObject();
                data.put("discord", account.discordAccountId());
                ctx.response().end(data.encode());
            } else {
                ctx.response().end(new JsonObject().put("error", "no links").encode());
            }
        }));
        router.get("/data/account/:id/profile").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            ctx.response().putHeader("Content-Type", "application/json");
            final Optional<Account> maybeAccount = mewna.accountManager().getAccountById(id);
            if(maybeAccount.isPresent()) {
                final Account account = maybeAccount.get();
                final JsonObject data = new JsonObject();
                data.put("id", account.id())
                        .put("username", account.username())
                        .put("displayName", account.displayName())
                        .put("avatar", account.avatar())
                        .put("aboutText", account.aboutText())
                        .put("customBackground", account.customBackground())
                        .put("ownedBackgroundPacks", account.ownedBackgroundPacks())
                        .put("isInBeta", account.isInBeta());
                ctx.response().end(data.encode());
            } else {
                ctx.response().end(new JsonObject().put("error", "no account").encode());
            }
        }));
        router.get("/data/account/:id/posts").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            ctx.response().putHeader("Content-Type", "application/json")
                    .end(new JsonArray(mewna.database().getLast100TimelinePosts(id)
                            .stream()
                            .map(JsonObject::mapFrom)
                            .collect(Collectors.toList()))
                            .encode());
        }));
        router.get("/data/account/:id/posts/all").handler(ctx -> move(() -> {
            final String id = ctx.request().getParam("id");
            ctx.response().putHeader("Content-Type", "application/json")
                    .end(new JsonArray(mewna.database().getAllTimelinePosts(id)
                            .stream()
                            .map(JsonObject::mapFrom)
                            .collect(Collectors.toList()))
                            .encode());
        }));
        router.post("/data/account/update")
                .handler(BodyHandler.create())
                .handler(ctx -> move(() -> {
                    final JsonObject body = ctx.getBodyAsJson();
                    mewna.accountManager().updateAccountSettings(body);
                    ctx.response().putHeader("Content-Type", "application/json").end(new JsonObject().encode());
                }));
        router.post("/data/account/update/oauth")
                .handler(BodyHandler.create())
                .handler(ctx -> move(() -> {
                    final JsonObject body = ctx.getBodyAsJson();
                    mewna.accountManager().createOrUpdateDiscordOAuthLinkedAccount(body);
                    ctx.response().putHeader("Content-Type", "application/json").end(new JsonObject().encode());
                }));
        router.get("/data/account/links/discord/:id").handler(ctx ->
                move(() -> ctx.response().end(mewna.accountManager()
                        .checkDiscordLinkedAccountExists(ctx.request().getParam("id")))));
    }
}
