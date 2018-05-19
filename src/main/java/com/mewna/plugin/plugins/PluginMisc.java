package com.mewna.plugin.plugins;

import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
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
@Plugin(name = "Misc", desc = "Miscellaneous things, like kittens and puppies.")
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
    
    @Command(names = {"help", "?"}, desc = "Get links to helpful information.", usage = "help", examples = "help")
    public void help(final CommandContext ctx) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("amybot help")
                .addField("Command listing", "https://amy.chat/commands", false)
                .addField("Bot invite", "https://amy.chat/invite", false)
                .addField("Support server", "https://amy.chat/support", false)
                .addField("", "This command: `amy!help`, `a.help`, or `a:help`", false);
        getMewna().getRestJDA().sendMessage(ctx.getChannel().getId(), builder.build()).queue(null, failure -> {
            if(failure instanceof ErrorResponseException) {
                //noinspection StatementWithEmptyBody
                if(((ErrorResponseException) failure).getErrorCode() == 50013) {
                    // TODO: We're missing a perm, do something about it
                    // TODO: Really should extract all this logic out somewhere else as a helper
                }
            }
        });
    }
    
    @Command(names = "invite", desc = "Get the invite link.", usage = "invite", examples = "invite")
    public void invite(final CommandContext ctx) {
        getMewna().getRestJDA().sendMessage(ctx.getChannel().getId(), "Click here: <https://amy.chat/invite>").queue();
    }
}
