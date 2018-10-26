package com.mewna.plugin.plugins;

import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.Role;
import com.mewna.cache.entity.User;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.guild.member.GuildMemberAddEvent;
import com.mewna.plugin.event.guild.member.GuildMemberRemoveEvent;
import com.mewna.plugin.plugins.settings.WelcomingSettings;
import com.mewna.util.Templater;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Welcoming", desc = "Hello and goodbye to your server's members.", settings = WelcomingSettings.class)
public class PluginWelcoming extends BasePlugin {
    private Templater map(final Guild guild, final User user) {
        final Map<String, String> data = new HashMap<>();
        final JsonObject jGuild = JsonObject.mapFrom(guild);
        for(final String key : jGuild.fieldNames()) {
            data.put("server." + key, jGuild.getMap().get(key).toString());
        }
        final JsonObject jUser = JsonObject.mapFrom(user);
        for(final String key : jUser.fieldNames()) {
            data.put("user." + key, jUser.getMap().get(key).toString());
        }
        data.put("user.mention", "<@" + user.getId() + '>');
        return Templater.fromMap(data);
    }
    
    @Event(EventType.GUILD_MEMBER_ADD)
    public void onUserJoin(final GuildMemberAddEvent event) {
        final Guild guild = event.getGuild();
        final String guildId = guild.getId();
        final WelcomingSettings settings = getMewna().getDatabase().getOrBaseSettings(WelcomingSettings.class, guildId);
        if(settings.isEnableWelcomeMessages()) {
            final String messageChannel = settings.getMessageChannel();
            // Have to validate that the chosen channel exists
            if(messageChannel != null && !messageChannel.isEmpty()) {
                final Channel channel = getMewna().getCache().getChannel(messageChannel);
                if(channel != null) {
                    final Templater templater = map(event.getGuild(), getMewna().getCache().getUser(event.getMember().getId()));
                    getCatnip().rest().channel().sendMessage(settings.getMessageChannel(), templater.render(settings.getWelcomeMessage()));
                } else {
                    getLogger().warn("Welcoming messageChannel {} in {} no longer valid, nulling...", messageChannel, guildId);
                    settings.setMessageChannel(null);
                    getDatabase().saveSettings(settings);
                }
            }
        }
        final String roleId = settings.getJoinRoleId();
        if(roleId != null && !roleId.isEmpty()) {
            // Validate that it exists
            final Role joinRole = getMewna().getCache().getRole(roleId);
            if(joinRole != null) {
                getCatnip().rest().guild().addGuildMemberRole(guildId, event.getMember().getId(), roleId);
            } else {
                getLogger().warn("Welcoming joinRole {} in {} no longer valid, nulling...", roleId, guildId);
                settings.setMessageChannel(null);
                getDatabase().saveSettings(settings);
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
                    getCatnip().rest().channel().sendMessage(settings.getMessageChannel(), templater.render(settings.getGoodbyeMessage()));
                } else {
                    getLogger().warn("Welcoming messageChannel {} in {} no longer valid, nulling...", messageChannel, guild.getId());
                    settings.setMessageChannel(null);
                    getDatabase().saveSettings(settings);
                }
            }
        }
    }
}
