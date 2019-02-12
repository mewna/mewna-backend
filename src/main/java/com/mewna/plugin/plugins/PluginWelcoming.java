package com.mewna.plugin.plugins;

import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.shard.DiscordEvent.Raw;
import com.mewna.event.discord.DiscordGuildMemberAdd;
import com.mewna.event.discord.DiscordGuildMemberRemove;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.plugins.settings.WelcomingSettings;
import com.mewna.util.Templater;
import io.sentry.Sentry;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Welcoming", desc = "Hello and goodbye to your server's members.", settings = WelcomingSettings.class)
public class PluginWelcoming extends BasePlugin {
    private Templater map(@SuppressWarnings("TypeMayBeWeakened") final Guild guild, final User user) {
        final Map<String, String> data = new HashMap<>();
        final JsonObject jGuild = JsonObject.mapFrom(guild);
        for(final String key : jGuild.fieldNames()) {
            data.put("server." + key, "" + jGuild.getValue(key));
        }
        final JsonObject jUser = JsonObject.mapFrom(guild);
        for(final String key : jUser.fieldNames()) {
            data.put("user." + key, "" + jUser.getValue(key));
        }
        data.put("user.name", user.username());
        data.put("user.mention", user.asMention());
        return Templater.fromMap(data);
    }
    
    @Event(Raw.GUILD_MEMBER_ADD)
    public void onUserJoin(final DiscordGuildMemberAdd event) {
        final Guild guild = event.guild();
        final String guildId = guild.id();
        database().getOrBaseSettings(WelcomingSettings.class, guildId).thenAccept(settings -> {
            if(settings.isEnableWelcomeMessages()) {
                final String messageChannel = settings.getMessageChannel();
                // TODO: Have to validate that the chosen channel exists
                if(messageChannel != null && !messageChannel.isEmpty()) {
                    final Templater templater = map(guild, event.user());
                    catnip().rest().channel().sendMessage(settings.getMessageChannel(), templater.render(settings.getWelcomeMessage()));
                }
            }
            final String roleId = settings.getJoinRoleId();
            if(roleId != null && !roleId.isEmpty()) {
                catnip().rest().guild().addGuildMemberRole(guildId, event.member().id(), roleId).exceptionally(e -> {
                    Sentry.capture(e);
                    return null;
                });
            }
        });
    }
    
    @Event(Raw.GUILD_MEMBER_REMOVE)
    public void onUserLeave(final DiscordGuildMemberRemove event) {
        final Guild guild = event.guild();
        database().getOrBaseSettings(WelcomingSettings.class, guild.id()).thenAccept(settings -> {
            if(settings.isEnableGoodbyeMessages()) {
                final String messageChannel = settings.getMessageChannel();
                // TODO: Have to validate that the chosen channel exists
                if(messageChannel != null && !messageChannel.isEmpty()) {
                    final Templater templater = map(event.guild(), event.user());
                    catnip().rest().channel().sendMessage(settings.getMessageChannel(), templater.render(settings.getGoodbyeMessage()));
                }
            }
        });
    }
}
