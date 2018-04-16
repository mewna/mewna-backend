package gg.cute.plugin.impl;

import gg.cute.plugin.BasePlugin;
import gg.cute.plugin.Command;
import gg.cute.plugin.CommandContext;
import gg.cute.plugin.Plugin;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;

/**
 * @author amy
 * @since 4/16/18.
 */
@Plugin("fun")
public class PluginFun extends BasePlugin {
    @Inject
    private OkHttpClient client;
    
    @Command(names = "cat", desc = "Get a random cat picture.", usage = "cat", examples = "cat")
    public void cat(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String cat = client.newCall(new Request.Builder().get().url("https://aws.random.cat/meow").build())
                    .execute().body().string();
            JSONObject json = new JSONObject(cat); // meow
            // TODO: Blah blah embeds lazy alesijkrcuthgfsn
            getRestJDA().sendMessage(ctx.getChannel(), json.getString("file")).queue();
        } catch(IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find cat :(").queue();
        }
    }
    
    @Command(names = "dog", desc = "Get a random dog picture.", usage = "dog", examples = "dog")
    public void dog(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String dog = client.newCall(new Request.Builder().get().url("https://random.dog/woof.json").build())
                    .execute().body().string();
            JSONObject json = new JSONObject(dog); // meow
            // TODO: Blah blah embeds lazy alesijkrcuthgfsn
            getRestJDA().sendMessage(ctx.getChannel(), json.getString("url")).queue();
        } catch(IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find dog :(").queue();
        }
    }
    
    @Command(names = "catgirl", desc = "Get a random (SFW) catgirl picture.", usage = "catgirl", examples = "catgirl")
    public void catgirl(final CommandContext ctx) {
        // TODO: Consider "real" NSFW variant? probably not, but...
        
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String catgirl = client.newCall(new Request.Builder().get().url("https://nekos.life/api/neko").build())
                    .execute().body().string();
            JSONObject json = new JSONObject(catgirl); // meow
            // TODO: Blah blah embeds lazy alesijkrcuthgfsn
            getRestJDA().sendMessage(ctx.getChannel(), json.getString("neko")).queue();
        } catch(IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find catgirl :(").queue();
        }
    }
}
