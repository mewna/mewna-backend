package gg.cute.plugin.impl;

import gg.cute.plugin.BasePlugin;
import gg.cute.plugin.Command;
import gg.cute.plugin.CommandContext;
import gg.cute.plugin.Plugin;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;

/**
 * @author amy
 * @since 4/8/18.
 */
@Plugin("utility")
public class PluginUtility extends BasePlugin {
    @Command(names = {"help", "?"}, desc = "Get links to helpful information.", usage = "help", examples = "help")
    public void help(final CommandContext ctx) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("amybot help")
                .addField("Command listing", "https://amy.chat/commands", false)
                .addField("Bot invite", "https://amy.chat/invite", false)
                .addField("Support server", "https://amy.chat/support", false)
                .addField("", "This command: `amy!help`, `a.help`, or `a:help`", false);
        getCute().getRestJDA().sendMessage(ctx.getChannel().getId(), builder.build()).queue(null, failure -> {
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
        getCute().getRestJDA().sendMessage(ctx.getChannel().getId(), "Click here: <https://amy.chat/invite>").queue();
    }
}
