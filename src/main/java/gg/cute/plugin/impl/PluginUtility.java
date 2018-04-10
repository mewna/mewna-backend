package gg.cute.plugin.impl;

import gg.cute.cache.entity.Channel;
import gg.cute.cache.entity.Guild;
import gg.cute.cache.entity.User;
import gg.cute.plugin.BasePlugin;
import gg.cute.plugin.Command;
import gg.cute.plugin.CommandContext;
import gg.cute.plugin.Plugin;

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
    
        StringBuilder sb = new StringBuilder("Success! You are `").append(user.getName()).append("` (")
                .append(user.asMention()).append(") triggering this command in `").append(guild.getName())
                .append("` channel `").append(channel.getName()).append("` (").append(channel.asMention())
                .append(").\nYou mentioned the following users:\n");
        mentions.forEach(e -> sb.append("* `").append(e.getName()).append("` (").append(e.asMention()).append(")\n"));
        
        getCute().getRestJDA().sendMessage(channel.getId(), sb.toString()).queue();
    }
}
