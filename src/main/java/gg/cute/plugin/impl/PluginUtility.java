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
    @Command(names = "test", desc = "Test command!")
    public void test(final CommandContext ctx) {
        final User user = ctx.getUser();
        final Channel channel = ctx.getChannel();
        final Guild guild = ctx.getGuild();
        final List<User> mentions = ctx.getMentions();
    
        final StringBuilder sb = new StringBuilder("Success! You are `").append(user.getName()).append("` (")
                .append(user.asMention()).append(") triggering this command in `").append(guild.getName())
                .append("` channel `").append(channel.getName()).append("` (").append(channel.asMention())
                .append(").\nYou mentioned the following users:\n");
        mentions.forEach(e -> sb.append("* `").append(e.getName()).append("` (").append(e.asMention()).append(")\n"));
        
        getCute().getRestJDA().sendMessage(channel.getId(), sb.toString()).queue();
    }
    
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
    
    @Command(names = {"main_name", "alias_1", "alias_2"},
            desc = "Your command description goes here. Used for auto-generated documentation.")
    public void myCommand(final CommandContext ctx) {
        getCute().getRestJDA().sendMessage(ctx.getChannel().getId(), "magic");
    }
}
