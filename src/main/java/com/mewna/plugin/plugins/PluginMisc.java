package com.mewna.plugin.plugins;

import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.plugins.settings.MiscSettings;
import net.dv8tion.jda.core.EmbedBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Objects;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Misc", desc = "Miscellaneous things, like kittens and puppies.", settings = MiscSettings.class)
public class PluginMisc extends BasePlugin {
    @Inject
    private OkHttpClient client;
    
    @Command(names = "cat", desc = "Get a random cat picture.", usage = "cat", examples = "cat")
    public void cat(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String cat = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://aws.random.cat/meow").build())
                    .execute().body()).string();
            final JSONObject json = new JSONObject(cat); // meow
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
            getRestJDA().sendMessage(ctx.getChannel(), new EmbedBuilder().setTitle("Dog").setImage(json.getString("url")).build()).queue();
        } catch(final IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find dog :(").queue();
        }
    }
    
    @Command(names = "catgirl", desc = "Get a random (SFW) catgirl picture.", usage = "catgirl", examples = "catgirl")
    public void catgirl(final CommandContext ctx) {
        try {
            @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
            final String catgirl = Objects.requireNonNull(client.newCall(new Request.Builder().get().url("https://nekos.life/api/neko").build())
                    .execute().body()).string();
            final JSONObject json = new JSONObject(catgirl); // nya
            getRestJDA().sendMessage(ctx.getChannel(), new EmbedBuilder().setTitle("Catgirl").setImage(json.getString("neko")).build()).queue();
        } catch(final IOException e) {
            getRestJDA().sendMessage(ctx.getChannel(), "Couldn't find catgirl :(").queue();
        }
    }
    
    @Command(names = {"help", "?"}, desc = "Get links to helpful information.", usage = "help", examples = "help")
    public void help(final CommandContext ctx) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Mewna help")
                .addField("Dashboard", System.getenv("DOMAIN"), false)
                .addField("Support server", "https://discord.gg/UwdDN6r", false)
                .addField("", "Everything can be enabled / disabled in the dashboard.", false)
        ;
        getRestJDA().sendMessage(ctx.getChannel().getId(), builder.build()).queue();
    }
}
