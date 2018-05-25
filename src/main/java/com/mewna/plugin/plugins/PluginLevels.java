package com.mewna.plugin.plugins;

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
import com.mewna.plugin.event.plugin.levels.LevelUpEvent;
import com.mewna.plugin.plugins.settings.LevelsSettings;
import com.mewna.util.Templater;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author amy
 * @since 5/19/18.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Plugin(name = "Levels", desc = "Allow gaining xp and leveling up by chatting.")
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
        while(xp >= levelToXp(level)) {
            xp -= levelToXp(level);
            level += 1;
        }
        
        return Math.max(0, level - 1);
    }
    
    private Templater map(final LevelUpEvent event) {
        final Guild guild = event.getGuild();
        final User user = event.getUser();
        final Map<String, String> data = new HashMap<>();
        final JSONObject jGuild = new JSONObject(guild);
        for(final String key : jGuild.keySet()) {
            data.put("server." + key, jGuild.get(key).toString());
        }
        final JSONObject jUser = new JSONObject(user);
        for(final String key : jUser.keySet()) {
            data.put("user." + key, jUser.get(key).toString());
        }
        data.put("user.mention", "<@" + user.getId() + '>');
        return Templater.fromMap(data);
    }
    
    @Event(EventType.LEVEL_UP)
    public void handleLevelUp(final LevelUpEvent event) {
        final Guild guild = event.getGuild();
        final LevelsSettings settings = getMewna().getDatabase().getOrBaseSettings(LevelsSettings.class, guild.getId());
        if(settings.isLevelsEnabled()) {
            if(settings.isLevelUpMessagesEnabled()) {
                // TODO: Handle cards
                // I guess worst-case we could CDN it and pretend instead of uploading to Discord
                // but that's gross af
                // TODO: Handle role rewards
                final String message = map(event).render(settings.getLevelUpMessage());
                getRestJDA().sendMessage(event.getChannel(), message).queue();
            }
        }
    }
    
    @Event(EventType.MESSAGE_CREATE)
    public void handleChatMessage(final MessageCreateEvent event) {
        final Guild guild = event.getGuild();
        final LevelsSettings settings = getMewna().getDatabase().getOrBaseSettings(LevelsSettings.class, guild.getId());
        if(!settings.isLevelsEnabled()) {
            return;
        }
        final User author = event.getAuthor();
        final Player player = getDatabase().getPlayer(author);
        getLogger().trace("Handling chat message for player {} in {}", author.getId(), guild.getId());
        
        // Calc. cooldown
        final ImmutablePair<Boolean, Long> localRes = getMewna().getRatelimiter()
                .checkUpdateRatelimit(event.getAuthor().getId(), "chat-xp-local:" + guild.getId(),
                        TimeUnit.MINUTES.toMillis(1));
        final ImmutablePair<Boolean, Long> globalRes = getMewna().getRatelimiter()
                .checkUpdateRatelimit(event.getAuthor().getId(), "chat-xp-global", TimeUnit.MINUTES.toMillis(1));
        
        if(!localRes.left) {
            final long oldXp = player.getXp(guild);
            final long xp = getXp(player);
            player.incrementLocalXp(guild, xp);
            getDatabase().savePlayer(player);
            getLogger().debug("Local XP: {} in {}: {} -> {}", author.getId(), guild.getId(), oldXp, oldXp + xp);
            if(isLevelUp(oldXp, oldXp + xp)) {
                getLogger().debug("{} in {}: Level up to {}", author.getId(), guild.getId(), xpToLevel(oldXp + xp));
                // Emit level-up event so we can process it
                getMewna().getNats().pushBackendEvent(EventType.LEVEL_UP, new JSONObject().put("user", author.getId())
                        .put("guild", guild.getId()).put("level", xpToLevel(oldXp + xp)).put("xp", oldXp + xp)
                        .put("channel", event.getChannel().getId()));
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
