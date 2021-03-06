package com.mewna.plugin;

import com.mewna.Mewna;
import com.mewna.cache.DiscordCache;
import com.mewna.cache.entity.Channel;
import com.mewna.data.Database;
import com.mewna.jda.RestJDA;
import lombok.AccessLevel;
import lombok.Getter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Random;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("unused")
public abstract class BasePlugin {
    @Inject
    @Getter(AccessLevel.PROTECTED)
    private Logger logger;
    @Inject
    @Getter(AccessLevel.PROTECTED)
    private Mewna mewna;
    @Inject
    @Getter(AccessLevel.PROTECTED)
    private Database database;
    @Inject
    @Getter(AccessLevel.PROTECTED)
    private Random random;
    @Inject
    @Getter(AccessLevel.PROTECTED)
    private DiscordCache cache;
    
    protected final RestJDA getRestJDA() {
        return getMewna().getRestJDA();
    }
    
    // TODO: Discord might get upset about API spam eventually I guess
    protected final void send(final Channel channel, final EmbedBuilder embed) {
        getRestJDA().sendMessage(channel.getId(), embed.build()).queue(null, failure -> {
            if(failure instanceof ErrorResponseException) {
                if(((ErrorResponseException) failure).getErrorCode() == 50013) {
                    // We're missing a perm (probably embeds), so send as plain text
                    getRestJDA().sendMessage(channel.getId(), embedToString(embed.build())).queue(innerFailure -> {
                        if(innerFailure instanceof ErrorResponseException) {
                            if(((ErrorResponseException) innerFailure).getErrorCode() == 50013) {
                                logger.error("Missing SEND_MESSAGES in channel #" + channel.getId());
                            }
                        }
                    });
                }
            }
        });
    }
    
    private String embedToString(final MessageEmbed e) {
        final StringBuilder sb = new StringBuilder();
        if(e.getTitle() != null) {
            sb.append("**").append(e.getTitle()).append("**").append('\n');
        }
        if(e.getDescription() != null) {
            sb.append(e.getDescription()).append('\n');
        }
        e.getFields().forEach(f -> sb.append("**").append(f.getName()).append("**\n").append(f.getValue()).append('\n'));
        sb.append("\n(Please give me the \"Embed Links\" permission so that this will look nicer!)");
        return sb.toString();
    }
}
