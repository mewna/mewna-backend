package com.mewna.plugin.plugins;

import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.guild.member.GuildMemberAddEvent;
import com.mewna.plugin.event.guild.member.GuildMemberRemoveEvent;
import com.mewna.plugin.plugins.settings.WelcomingSettings;
import com.mewna.util.Templater;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Welcoming", desc = "Hello and goodbye to your server's members.")
public class PluginWelcoming extends BasePlugin {
    private Templater map(final Guild guild, final User user) {
        final Map<String, String> data = new HashMap<>();
        final JSONObject jGuild = new JSONObject(guild);
        for(final String key : jGuild.keySet()) {
            data.put("server." + key, jGuild.get(key).toString());
        }
        final JSONObject jUser = new JSONObject(user);
        for(final String key : jUser.keySet()) {
            data.put("user." + key, jUser.get(key).toString());
        }
        return Templater.fromMap(data);
    }
    
    @Event(EventType.GUILD_MEMBER_ADD)
    public void onUserJoin(final GuildMemberAddEvent event) {
        final Guild guild = event.getGuild();
        final WelcomingSettings settings = getMewna().getDatabase().getOrBaseSettings(WelcomingSettings.class, guild.getId());
        if(settings.isEnableWelcomeMessages()) {
            final String messageChannel = settings.getMessageChannel();
            // Have to validate that the chosen channel exists
            if(messageChannel != null && !messageChannel.isEmpty()) {
                final Channel channel = getMewna().getCache().getChannel(messageChannel);
                if(channel != null) {
                    final Templater templater = map(event.getGuild(), getMewna().getCache().getUser(event.getMember().getId()));
                    getRestJDA().sendMessage(settings.getMessageChannel(), templater.render(settings.getWelcomeMessage())).queue();
                } else {
                    getLogger().warn("Welcoming messageChannel {} in {} no longer valid, nulling...", messageChannel, guild.getId());
                    settings.setMessageChannel(null);
                    getDatabase().saveSettings(settings);
                }
            }
        }
    }
    
    @Event(EventType.GUILD_MEMBER_REMOVE)
    public void onUserLeave(final GuildMemberRemoveEvent event) {
        final Guild guild = event.getGuild();
        final WelcomingSettings settings = getMewna().getDatabase().getOrBaseSettings(WelcomingSettings.class, guild.getId());
        if(settings.isEnableGoodbyeMessages()) {
            final String messageChannel = settings.getMessageChannel();
            // Have to validate that the chosen channel exists
            if(messageChannel != null && !messageChannel.isEmpty()) {
                final Channel channel = getMewna().getCache().getChannel(messageChannel);
                if(channel != null) {
                    final Templater templater = map(event.getGuild(), event.getUser());
                    getRestJDA().sendMessage(settings.getMessageChannel(), templater.render(settings.getGoodbyeMessage())).queue();
                } else {
                    getLogger().warn("Welcoming messageChannel {} in {} no longer valid, nulling...", messageChannel, guild.getId());
                    settings.setMessageChannel(null);
                    getDatabase().saveSettings(settings);
                }
            }
        }
    }
}
