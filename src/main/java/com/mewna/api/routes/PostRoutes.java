package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.data.account.Account;
import com.mewna.data.account.timeline.TimelinePost;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mewna.util.Async.move;

/**
 * @author amy
 * @since 4/15/19.
 */
public class PostRoutes implements RouteGroup {
    private JsonObject toAuthor(final Account account) {
        return new JsonObject()
                .put("id", account.id())
                .put("name", account.displayName())
                .put("avatar", account.avatar())
                .put("username", account.username())
                ;
    }
    
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.post("/v3/post/:id/create").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final String id = ctx.pathParam("id");
            final TimelinePost post = TimelinePost.create(id, false, ctx.getBodyAsString());
            final boolean worked = mewna.database().saveTimelinePost(post);
            if(worked) {
                final JsonObject out = new JsonObject().put("id", post.getId());
                ctx.response().end(out.encode());
            } else {
                ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("invalid post"))).encode());
            }
        }));
        router.get("/v3/post/author/:id").handler(ctx -> move(() -> {
            final String id = ctx.pathParam("id");
            final Optional<Account> account = mewna.database().getAccountById(id);
            if(account.isPresent()) {
                ctx.response().end(toAuthor(account.get()).encode());
            } else {
                ctx.response().end(new JsonObject().encode());
            }
        }));
        router.get("/v3/post/:id/posts").handler(ctx -> move(() -> {
            final String id = ctx.pathParam("id");
            
            final List<String> limits = ctx.queryParam("limit");
            final List<String> afters = ctx.queryParam("after");
            int limit = 100;
            if(!limits.isEmpty()) {
                limit = Integer.parseInt(limits.get(0));
            }
            long after = 0;
            if(!afters.isEmpty()) {
                after = Integer.parseInt(afters.get(0));
            }
            
            final JsonArray out = new JsonArray(mewna.database().getTimelinePostChunk(id, limit, after));
            ctx.response().end(out.encode());
        }));
        router.post("/v3/homepage").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            // Honestly this should probably just be a GET but I didn't wanna build query strings.
            final List<String> ids = ctx.getBodyAsJsonArray().stream()
                    .map(e -> (String) e)
                    .collect(Collectors.toList());
            final List<String> limits = ctx.queryParam("limit");
            final List<String> afters = ctx.queryParam("after");
            int limit = 100;
            if(!limits.isEmpty()) {
                limit = Integer.parseInt(limits.get(0));
            }
            long after = 0;
            if(!afters.isEmpty()) {
                after = Integer.parseInt(afters.get(0));
            }
    
            final JsonArray out = new JsonArray(mewna.database().getPostsFromIds(ids, limit, after));
            ctx.response().end(out.encode());
        }));
        router.get("/v3/post/:id/:post").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final String id = ctx.pathParam("post");
            final JsonObject out = mewna.database().getTimelinePost(id).map(JsonObject::mapFrom).orElse(new JsonObject());
            ctx.response().end(out.encode());
        }));
        router.delete("/v3/post/:id/:post").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final String id = ctx.pathParam("post");
            final boolean worked = mewna.database().deleteTimelinePost(id);
            if(worked) {
                ctx.response().end(new JsonObject().encode());
            } else {
                ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("can't delete post"))).encode());
            }
        }));
        router.put("/v3/post/:id/:post").handler(BodyHandler.create()).handler(ctx -> move(() -> {
            final String id = ctx.pathParam("post");
            final TimelinePost post = TimelinePost.create(null, false, ctx.getBodyAsString());
            post.setId(id);
            final boolean worked = mewna.database().updateTimelinePost(post);
            if(worked) {
                final JsonObject out = new JsonObject().put("id", post.getId());
                ctx.response().end(out.encode());
            } else {
                ctx.response().end(new JsonObject().put("errors", new JsonArray(List.of("invalid post"))).encode());
            }
        }));
        
        /*
        router.get("/data/blog/:id/posts/all").handler(ctx -> {
            // Get all posts for a server
            move(() -> ctx.response().end(new JsonArray(mewna.database().getServerBlogPosts(ctx.request().getParam("id"))).encode()));
        });
        router.get("/data/blog/:id/posts/all/titles").handler(ctx -> {
            // Get all posts for a server
            move(() -> ctx.response().end(mewna.database().getServerBlogPostTitles(ctx.request().getParam("id")).encode()));
        });
        
        router.post("/data/blog/:id/post/:post/boop").handler(BodyHandler.create()).handler(ctx -> {
            // Boop a post for a given user
            // TODO
            move(() -> {
            
            });
        });
        router.delete("/data/blog/:id/post/:post/boop").handler(BodyHandler.create()).handler(ctx -> {
            // Un-boop a post for a given user
            // TODO
            move(() -> {
            
            });
        });
        */
    }
}
