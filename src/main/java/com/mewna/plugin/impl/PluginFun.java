package com.mewna.plugin.impl;

import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import net.dv8tion.jda.core.EmbedBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Objects;

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
            final String cat = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://aws.random.cat/meow").build())
                    .execute().body()).string();
            final JSONObject json = new JSONObject(cat); // meow
            // TODO: Blah blah embeds lazy alesijkrcuthgfsn
            getRestJDA().sendMessage(ctx.getChannel(), new EmbedBuilder().setTitle("Cat").setImage(json.getString("file")).build()).queue();
        } catch(final IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find cat :(").queue();
        }
    }
    
    @Command(names = "dog", desc = "Get a random dog picture.", usage = "dog", examples = "dog")
    public void dog(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String dog = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://random.dog/woof.json").build())
                    .execute().body()).string();
            final JSONObject json = new JSONObject(dog); // woof
            // TODO: Blah blah embeds lazy alesijkrcuthgfsn
            getRestJDA().sendMessage(ctx.getChannel(), new EmbedBuilder().setTitle("Dog").setImage(json.getString("url")).build()).queue();
        } catch(final IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find dog :(").queue();
        }
    }
    
    @Command(names = "catgirl", desc = "Get a random (SFW) catgirl picture.", usage = "catgirl", examples = "catgirl")
    public void catgirl(final CommandContext ctx) {
        // TODO: Consider "real" NSFW variant? probably not, but...
        
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String catgirl = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://nekos.life/api/neko").build())
                    .execute().body()).string();
            final JSONObject json = new JSONObject(catgirl); // nya
            // TODO: Blah blah embeds lazy alesijkrcuthgfsn
            getRestJDA().sendMessage(ctx.getChannel(), new EmbedBuilder().setTitle("Catgirl").setImage(json.getString("neko")).build()).queue();
        } catch(final IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find catgirl :(").queue();
        }
    }
}
