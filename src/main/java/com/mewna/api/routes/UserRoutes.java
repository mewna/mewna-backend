package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.util.ImageOptions;
import com.mewna.data.account.Account;
import com.mewna.data.account.PremiumAccount;
import com.mewna.data.cache.DiscordCache;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.List;
import java.util.Optional;

import static com.mewna.util.Async.move;

/**
 * @author amy
 * @since 4/18/19.
 */
public class UserRoutes implements RouteGroup {
    private JsonObject minify(final User user) {
        if(user == null) {
            return new JsonObject();
        }
        return new JsonObject()
                .put("id", user.id())
                .put("username", user.username())
                .put("discriminator", user.discriminator())
                .put("avatar", user.effectiveAvatarUrl(new ImageOptions().png().size(128)))
                .put("bot", user.bot());
    }
    
    private JsonObject minify(final Account account) {
        if(account == null) {
            return new JsonObject();
        }
        return new JsonObject()
                .put("id", account.id())
                .put("displayName", account.displayName())
                .put("avatar", account.avatar())
                .put("customBackground", account.customBackground())
                .put("aboutText", account.aboutText())
                .put("banned", account.banned())
                .put("username", account.username())
                .put("banReason", account.banReason())
                .put("discordId", account.discordAccountId())
                .put("beta", account.isInBeta())
                .put("premium", account.premium())
                ;
    }
    
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.get("/v3/user/:id").handler(ctx -> move(() -> {
            final String id = ctx.pathParam("id");
            final Optional<Account> account = mewna.database().getAccountById(id);
            final User user = DiscordCache.user(id);
            final JsonObject miniAccount = minify(account.orElse(null));
            final JsonObject miniUser = minify(user);
            ctx.response().end(miniAccount.put("discord", miniUser).encode());
        }));
        router.post("/v3/user/:id").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final String id = ctx.pathParam("id");
            final Optional<Account> account = mewna.database().getAccountById(id);
            final JsonObject patch = ctx.getBodyAsJson();
            if(account.isPresent()) {
                if(account.get().updateSettings(mewna.database(), id, patch)) {
                    ctx.response().end(new JsonObject().put("status", "ok").encode());
                } else {
                    ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("invalid patch"))).encode());
                }
            } else {
                ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("no account"))).encode());
            }
        }));
        router.get("/v3/user/:id/premium").handler(ctx -> move(() -> {
            final String id = ctx.pathParam("id");
            final Optional<Account> maybeAccount = mewna.database().getAccountById(id);
            if(maybeAccount.isPresent()) {
                final Account account = maybeAccount.get();
                if(account.premium()) {
                    final Optional<PremiumAccount> maybeData = mewna.database().getPremiumData(account);
                    final PremiumAccount data;
                    if(maybeData.isPresent()) {
                        data = maybeData.get();
                    } else {
                        data = new PremiumAccount();
                        data.id(account.id());
                        mewna.database().savePremiumData(data);
                    }
                    ctx.response().end(JsonObject.mapFrom(data).encode());
                } else {
                    ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("not premium"))).encode());
                }
            } else {
                ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("no account"))).encode());
            }
        }));
        router.post("/v3/user/:id/premium").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final String id = ctx.pathParam("id");
            final Optional<Account> maybeAccount = mewna.database().getAccountById(id);
            if(maybeAccount.isPresent()) {
                final Account account = maybeAccount.get();
                if(account.premium()) {
                    final Optional<PremiumAccount> maybeData = mewna.database().getPremiumData(account);
                    final PremiumAccount data;
                    if(maybeData.isPresent()) {
                        data = maybeData.get();
                    } else {
                        data = new PremiumAccount();
                        data.id(account.id());
                        mewna.database().savePremiumData(data);
                    }
                    ctx.response().end(JsonObject.mapFrom(data).encode());
                } else {
                    ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("not premium"))).encode());
                }
            } else {
                ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("no account"))).encode());
            }
        }));
    }
}
