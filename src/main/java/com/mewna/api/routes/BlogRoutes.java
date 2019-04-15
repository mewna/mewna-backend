package com.mewna.api.routes;

import com.mewna.Mewna;
import com.mewna.api.RouteGroup;
import com.mewna.servers.ServerBlogPost;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import static com.mewna.util.Async.move;

/**
 * @author amy
 * @since 2/10/19.
 */
public class BlogRoutes implements RouteGroup {
    @Override
    public void registerRoutes(final Mewna mewna, final Router router) {
        router.post("/data/blog/:id/post").handler(BodyHandler.create()).handler(ctx -> {
            // Create a post
            move(() -> ctx.response().end(new JsonObject().put("id", mewna.database()
                    .saveNewServerBlogPost(ctx.getBodyAsJson().mapTo(ServerBlogPost.class)))
                    .encode()));
        });
        router.get("/data/blog/:id/post/:post").handler(BodyHandler.create()).handler(ctx -> {
            // Get a single post
            move(() -> ctx.response().end(mewna.database().getServerBlogPostById(ctx.request().getParam("post"))
                    .map(ServerBlogPost::toJson).orElse(new JsonObject()).encode()));
        });
        router.delete("/data/blog/:id/post/:post").handler(BodyHandler.create()).handler(ctx -> {
            // Delete a single post
            move(() -> {
                mewna.database().deleteServerBlogPost(ctx.request().getParam("post"));
                ctx.response().end("");
            });
        });
        router.put("/data/blog/:id/post/:post").handler(BodyHandler.create()).handler(ctx -> {
            // Edit a single post
            move(() -> ctx.response().end(new JsonObject().put("id", mewna.database()
                    .updateServerBlogPost(ctx.getBodyAsJson().mapTo(ServerBlogPost.class)))
                    .encode()));
        });
        router.get("/data/blog/:id/posts").handler(ctx -> {
            // Get last 100 posts for a server
            move(() -> ctx.response().end(new JsonArray(mewna.database().getLast100ServerBlogPosts(ctx.request().getParam("id"))).encode()));
        });
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
    }
}
