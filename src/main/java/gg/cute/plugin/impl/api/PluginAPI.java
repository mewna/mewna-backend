package gg.cute.plugin.impl.api;

import gg.cute.plugin.BasePlugin;
import gg.cute.plugin.Plugin;

import static spark.Spark.*;

/**
 * @author amy
 * @since 4/25/18.
 */
@Plugin("api")
public class PluginAPI extends BasePlugin {
    @Override
    public void finishLoading() {
        // TODO: Allow overriding
        port(8080);
        webSocket("/socket", SocketAPI.class);
        get("/", (req, res) -> {
            return "api? yes!";
        });
        
        after((req, res) -> {
            res.header("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
            res.header("Access-Control-Allow-Credentials", "true");
        });
    }
}
