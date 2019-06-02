package com.mewna.data.cache;

import com.mewna.Mewna;
import com.mewna.catnip.entity.channel.Channel;
import com.mewna.catnip.entity.channel.TextChannel;
import com.mewna.catnip.entity.channel.VoiceChannel;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.guild.Role;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.user.VoiceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author amy
 * @since 10/31/18.
 */
@SuppressWarnings({"unused", "ConstantConditions"})
public final class DiscordCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscordCache.class);
    
    private DiscordCache() {
    }
    
    public static Guild guild(final String id) {
        try {
            return Mewna.getInstance().catnip().cache().guild(id);
        } catch(final Exception e) {
            return null;
        }
    }
    
    public static User user(final String id) {
        return Mewna.getInstance().catnip().cache().user(id);
    }
    
    public static TextChannel textChannel(final String guild, final String id) {
        try {
            return Mewna.getInstance().catnip().cache().channel(guild, id).asTextChannel();
        } catch(final Exception e) {
            return null;
        }
    }
    
    public static VoiceChannel voiceChannel(final String guild, final String id) {
        try {
            return Mewna.getInstance().catnip().cache().channel(guild, id).asVoiceChannel();
        } catch(final Exception e) {
            return null;
        }
    }
    
    public static Collection<TextChannel> channels(final String guild) {
        return Mewna.getInstance().catnip()
                .cache()
                .channels(guild)
                .stream()
                .filter(e -> e instanceof TextChannel)
                .map(Channel::asTextChannel)
                .collect(Collectors.toList());
    }
    
    public static Role role(final String guild, final String id) {
        return Mewna.getInstance().catnip().cache().role(guild, id);
    }
    
    public static Collection<Role> roles(final String guild) {
        return Mewna.getInstance().catnip().cache()
                .roles(guild)
                .stream()
                .collect(Collectors.toList());
    }
    
    public static Member member(final String guild, final String id) {
        return Mewna.getInstance().catnip().cache().member(guild, id);
    }
    
    public static VoiceState voiceState(final String guild, final String id) {
        return Mewna.getInstance().catnip().cache().voiceState(guild, id);
    }
}
