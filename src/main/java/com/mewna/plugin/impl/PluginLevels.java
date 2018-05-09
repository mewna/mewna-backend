package com.mewna.plugin.impl;

import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.data.Player;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.message.MessageCreateEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.concurrent.TimeUnit;

/**
 * @author amy
 * @since 4/16/18.
 */
@Plugin("levels")
@SuppressWarnings("unused")
public class PluginLevels extends BasePlugin {
    static boolean isLevelUp(final long oldXp, final long xp) {
        return xpToLevel(oldXp) < xpToLevel(xp);
    }
    
    static long levelToXp(final long level) {
        return Math.max(0, 100 * level + 20 * (level - 1));
    }
    
    static long fullLevelToXp(final long level) {
        long requiredXp = 0;
        for(int i = 1; i <= level; i++) {
            requiredXp += levelToXp(i);
        }
        return requiredXp;
    }
    
    static long nextLevelXp(final long xp) {
        return fullLevelToXp(xpToLevel(xp) + 1) - xp;
    }
    
    static long xpToLevel(long xp) {
        long level = 0;
        //noinspection StatementWithEmptyBody
        while(true) {
            if(xp < levelToXp(level)) {
                break;
            }
            xp -= levelToXp(level);
            level += 1;
        }
        
        return Math.max(0, level - 1);
    }
    
    @Event(EventType.MESSAGE_CREATE)
    public void handleChatMessage(final MessageCreateEvent event) {
        final User author = event.getAuthor();
        final Player player = getDatabase().getPlayer(author);
        final Guild guild = event.getGuild();
        getLogger().trace("Handling chat message for player {} in {}", author.getId(), guild.getId());
        
        // Calc. cooldown
        final ImmutablePair<Boolean, Long> localRes = this.getMewna().getRatelimiter()
                .checkUpdateRatelimit(event.getAuthor().getId(), "chat-xp-local:" + guild.getId(),
                        TimeUnit.MINUTES.toMillis(1));
        final ImmutablePair<Boolean, Long> globalRes = this.getMewna().getRatelimiter()
                .checkUpdateRatelimit(event.getAuthor().getId(), "chat-xp-global", TimeUnit.MINUTES.toMillis(1));
        
        if(!localRes.left) {
            final long oldXp = player.getXp(guild);
            final long xp = getXp(player);
            player.incrementLocalXp(guild, xp);
            getDatabase().savePlayer(player);
            getLogger().trace("Local XP: {} in {}: {} -> {}", author.getId(), guild.getId(), oldXp, oldXp + xp);
            if(isLevelUp(oldXp, oldXp + xp)) {
                // TODO: Level-up notification
                getLogger().debug("{} in {}: Level up to {}", author.getId(), guild.getId(), xpToLevel(oldXp + xp));
            }
        }
        if(!globalRes.left) {
            player.incrementGlobalXp(getXp(player));
            getDatabase().savePlayer(player);
            // TODO: Level-up notifications here?
        }
    }
    
    @Command(names = {"rank", "level"}, desc = "Check your rank, or someone else's rank.", usage = "rank [@mention]",
            examples = {"rank", "rank @someone"})
    public void rank(final CommandContext ctx) {
        // TODO: Exp-level calculations go here
    }
    
    @Command(names = {"leaderboards", "ranks", "levels", "leaderboard", "rankings"}, desc = "View the guild leaderboards.",
            usage = "leaderboards", examples = "leaderboards")
    public void ranks(final CommandContext ctx) {
        getRestJDA().sendMessage(ctx.getChannel(), "https://amy.chat/leaderboards/" + ctx.getGuild().getId()).queue();
    }
    
    private long getXp(final Player player) {
        return 10 + getRandom().nextInt(10);
    }
}
