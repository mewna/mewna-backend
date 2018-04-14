package gg.cute.plugin.impl;

import gg.cute.cache.entity.Channel;
import gg.cute.cache.entity.Guild;
import gg.cute.cache.entity.User;
import gg.cute.plugin.BasePlugin;
import gg.cute.plugin.Command;
import gg.cute.plugin.CommandContext;
import gg.cute.plugin.Plugin;
import net.dv8tion.jda.core.EmbedBuilder;

import java.util.List;

/**
 * @author amy
 * @since 4/8/18.
 */
@Plugin("utility")
public class PluginUtility extends BasePlugin {
    @Command(names = {"help", "?"}, desc = "Get links to helpful information.")
    public void help(final CommandContext ctx) {
        final EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("amybot help")
                .addField("Command listing", "https://amy.chat/commands", false)
                .addField("Bot invite", "https://amy.chat/invite", false)
                .addField("Support server", "https://amy.chat/support", false)
                .addField("", "This command: `amy!help`, `a.help`, or `a:help`", false);
        getCute().getRestJDA().sendMessage(ctx.getChannel().getId(), builder.build()).queue();
    }
    
    @Command(names = "invite", desc = "Get the invite link.")
    public void invite(final CommandContext ctx) {
        getCute().getRestJDA().sendMessage(ctx.getChannel().getId(), "Click here: <https://amy.chat/invite>").queue();
    }
}
