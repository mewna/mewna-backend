package com.mewna.plugin.plugins;

import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.Member;
import com.mewna.cache.entity.User;
import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.rest.guild.MemberData;
import com.mewna.data.Player;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.message.MessageCreateEvent;
import com.mewna.plugin.event.plugin.behaviour.PlayerEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemUserEventType;
import com.mewna.plugin.event.plugin.levels.LevelUpEvent;
import com.mewna.plugin.plugins.settings.LevelsSettings;
import com.mewna.plugin.util.Emotes;
import com.mewna.plugin.util.Renderer;
import com.mewna.util.Templater;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.awt.*;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author amy
 * @since 5/19/18.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Plugin(name = "Levels", desc = "Allow gaining xp and leveling up by chatting.", settings = LevelsSettings.class)
public class PluginLevels extends BasePlugin {
    public static boolean isLevelUp(final long oldXp, final long xp) {
        return xpToLevel(oldXp) < xpToLevel(xp);
    }
    
    public static long levelToXp(final long level) {
        return Math.max(0, 100 * level + 20 * (level - 1));
    }
    
    public static long fullLevelToXp(final long level) {
        long requiredXp = 0;
        for(int i = 1; i <= level; i++) {
            requiredXp += levelToXp(i);
        }
        return requiredXp;
    }
    
    public static long nextLevelXp(final long xp) {
        return fullLevelToXp(xpToLevel(xp) + 1) - xp;
    }
    
    public static long xpToLevel(long xp) {
        long level = 0;
        while(xp >= levelToXp(level)) {
            xp -= levelToXp(level);
            level += 1;
        }
        
        return Math.max(0, level - 1);
    }
    
    public static int getAllRankedPlayersInGuild(final Guild guild) {
        final int[] count = {0};
        Mewna.getInstance().getDatabase().getStore().sql("SELECT COUNT(*) AS count FROM players WHERE data->'guildXp'->'" + guild.getId() + "' IS NOT NULL;", p -> {
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                resultSet.next();
            }
            count[0] = resultSet.getInt("count");
        });
        return count[0];
    }
    
    public static int getPlayerRankInGuild(final Guild guild, final User player) {
        final int[] rank = {-1};
        final String guildId = guild.getId();
        final String playerId = player.getId();
        Mewna.getInstance().getDatabase().getStore().sql("SELECT rank FROM (SELECT row_number() OVER (" +
                "ORDER BY (data->'guildXp'->>'" + guildId + "')::integer DESC" +
                ") AS rank, data FROM players " +
                "WHERE data->'guildXp'->'" + guildId + "' IS NOT NULL " +
                ") AS _q " +
                "WHERE data->>'id' = '" + playerId + "';", p -> {
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                resultSet.next();
                rank[0] = resultSet.getInt("rank");
            } else {
                rank[0] = 1;
            }
        });
        return rank[0];
    }
    
    public static int getPlayerRankGlobally(final User player) {
        final int[] rank = {-1};
        final String playerId = player.getId();
        Mewna.getInstance().getDatabase().getStore().sql("SELECT rank FROM (SELECT row_number() OVER (" +
                "ORDER BY (data->>'globalXp')::integer DESC" +
                ") AS rank, data FROM players) AS _q " +
                "WHERE data->>'id' = '" + playerId + "';", p -> {
            final ResultSet resultSet = p.executeQuery();
            if(resultSet.isBeforeFirst()) {
                resultSet.next();
                rank[0] = resultSet.getInt("rank");
            } else {
                rank[0] = -1;
            }
        });
        return rank[0];
    }
    
    private Templater map(final LevelUpEvent event) {
        final Guild guild = event.getGuild();
        final User user = event.getUser();
        final Map<String, String> data = new HashMap<>();
        final JsonObject jGuild = JsonObject.mapFrom(guild);
        for(final String key : jGuild.fieldNames()) {
            data.put("server." + key, jGuild.getMap().get(key).toString());
        }
        final JsonObject jUser = JsonObject.mapFrom(user);
        for(final String key : jUser.fieldNames()) {
            data.put("user." + key, jUser.getMap().get(key).toString());
        }
        data.put("user.mention", user.asMention());
        data.put("level", event.getLevel() + "");
        data.put("xp", event.getXp() + "");
        return Templater.fromMap(data);
    }
    
    @Event(EventType.LEVEL_UP)
    public void handleLevelUp(final LevelUpEvent event) {
        final Guild guild = event.getGuild();
        getMewna().getStatsClient().count("discord.backend.levelups", 1);
        final LevelsSettings settings = getMewna().getDatabase().getOrBaseSettings(LevelsSettings.class, guild.getId());
        if(settings.isLevelsEnabled()) {
            final Member member = getCache().getMember(guild, event.getUser());
            if(settings.isLevelUpMessagesEnabled()) {
                sendLevelUpMessage(settings, event, member);
            }
            if(settings.isRemovePreviousRoleRewards()) {
                removeAndAddRoleRewards(settings, guild, member, event.getLevel(), () -> {
                });
            } else {
                addRoleRewards(settings, guild, member, event.getLevel(), () -> {
                });
            }
        }
    }
    
    private void sendLevelUpMessage(final LevelsSettings settings, final LevelUpEvent event, final Member member) {
        if(settings.isLevelsEnabled()) {
            if(settings.isLevelUpMessagesEnabled()) {
                final String message = map(event).render(settings.getLevelUpMessage());
                getCatnip().rest().channel().sendMessage(event.getChannel().getId(), message);
            }
        }
    }
    
    private void removeAndAddRoleRewards(final LevelsSettings settings, final Guild guild, final Member member,
                                         final long level, final Runnable callback) {
        // Check for roles at this level
        final List<String> rewards = settings.getLevelRoleRewards().entrySet().stream()
                .filter(e -> e.getValue() == level).map(Entry::getKey).collect(Collectors.toList());
        if(!rewards.isEmpty()) {
            // If we have some, remove lower roles then add in the rest
            final List<String> removeRoles = settings.getLevelRoleRewards().entrySet().stream()
                    .filter(e -> e.getValue() < level).map(Entry::getKey).collect(Collectors.toList());
            final Collection<String> endRoles = new HashSet<>(member.getRoles());
            endRoles.addAll(rewards);
            endRoles.removeAll(removeRoles);
            final MemberData memberData = new MemberData();
            //noinspection ResultOfMethodCallIgnored
            endRoles.forEach(memberData::addRole);
            getCatnip().rest().guild().modifyGuildMember(guild.getId(), member.getId(), memberData)
                    .thenAccept(__ -> callback.run());
        }
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void addRoleRewards(final LevelsSettings settings, final Guild guild, final Member member, final long level,
                                final Runnable callback) {
        // Check for roles at or below this level
        final List<String> rewards = settings.getLevelRoleRewards().entrySet().stream()
                .filter(e -> e.getValue() <= level).map(Entry::getKey).collect(Collectors.toList());
        if(!rewards.isEmpty()) {
            final MemberData memberData = new MemberData();
            member.getRoles().forEach(memberData::addRole);
            rewards.forEach(memberData::addRole);
            getCatnip().rest().guild().modifyGuildMember(guild.getId(), member.getId(), memberData)
                    .thenAccept(__ -> callback.run());
        }
    }
    
    @Event(EventType.MESSAGE_CREATE)
    public void handleChatMessage(final MessageCreateEvent event) {
        final User author = event.getAuthor();
        final Player player = getDatabase().getPlayer(author);
        final ImmutablePair<Boolean, Long> globalRes = getMewna().getRatelimiter()
                .checkUpdateRatelimit(author.getId(), "chat-xp-global", TimeUnit.MINUTES.toMillis(10));
        if(!globalRes.left) {
            final long oldXp = player.getGlobalXp();
            final long xp = getXp(player);
            getMewna().getStatsClient().count("discord.backend.xpgained.global", xp);
            player.incrementGlobalXp(getXp(player));
            getDatabase().savePlayer(player);
            // Level-up notifications here?
            if(isLevelUp(oldXp, oldXp + xp)) {
                final long level = xpToLevel(oldXp + xp);
                // lol
                switch((int) level) {
                    case 10:
                    case 25:
                    case 50:
                    case 100: {
                        getMewna().getPluginManager().processEvent(EventType.PLAYER_EVENT,
                                new PlayerEvent(SystemUserEventType.GLOBAL_LEVEL, player,
                                        new JsonObject().put("level", level)));
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
        
        final Guild guild = event.getGuild();
        final LevelsSettings settings = getMewna().getDatabase().getOrBaseSettings(LevelsSettings.class, guild.getId());
        if(!settings.isLevelsEnabled()) {
            return;
        }
        getLogger().trace("Handling chat message for player {} in {}", author.getId(), guild.getId());
        
        final ImmutablePair<Boolean, Long> localRes = getMewna().getRatelimiter()
                .checkUpdateRatelimit(event.getAuthor().getId(), "chat-xp-local:" + guild.getId(),
                        TimeUnit.MINUTES.toMillis(1));
        if(!localRes.left) {
            final long oldXp = player.getXp(guild);
            final long xp = getXp(player);
            getMewna().getStatsClient().count("discord.backend.xpgained.local", xp);
            player.incrementLocalXp(guild, xp);
            getDatabase().savePlayer(player);
            getLogger().debug("Local XP: {} in {}: {} -> {}", author.getId(), guild.getId(), oldXp, oldXp + xp);
            if(isLevelUp(oldXp, oldXp + xp)) {
                getLogger().debug("{} in {}: Level up to {}", author.getId(), guild.getId(), xpToLevel(oldXp + xp));
                // Emit level-up event so we can process it
                // TODO: Singyeong messages
                /*
                getMewna().getNats().pushBackendEvent(EventType.LEVEL_UP, new JsonObject().put("user", author.getId())
                        .put("guild", guild.getId()).put("level", xpToLevel(oldXp + xp)).put("xp", oldXp + xp)
                        .put("channel", event.getChannel().getId()));
                        */
            }
        } else {
            getLogger().debug("Local XP: {} in {} ratelimited ({}ms)", author.getId(), guild.getId(),
                    getMewna().getRatelimiter().getRatelimitTime(author.getId(), "chat-xp-local:" + guild.getId(),
                            TimeUnit.MINUTES.toMillis(1)));
        }
    }
    
    @Command(names = {"rank", "level"}, desc = "commands.levels.rank", usage = "rank [@mention]",
            examples = {"rank", "rank @someone"})
    public void rank(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        final LevelsSettings settings = getMewna().getDatabase().getOrBaseSettings(LevelsSettings.class, guild.getId());
        if(!settings.isLevelsEnabled()) {
            getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(), "Levels are not enabled in this server. " +
                    "The server owner (or administrators) should enable them in the dashboard: <https://mewna.com/>")
            ;
            return;
        }
        final User user;
        final Player player;
        if(ctx.getMentions().isEmpty()) {
            user = ctx.getUser();
            player = ctx.getPlayer();
        } else {
            user = ctx.getMentions().get(0);
            player = getDatabase().getPlayer(user);
        }
        
        if(user.isBot()) {
            getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(), "Bots can't have levels!");
            return;
        }
        
        getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                Emotes.LOADING_ICON + " Generating rank card (this will take a few seconds)")
                .thenAccept(message -> getCatnip().rest().channel().triggerTypingIndicator(ctx.getChannel().getId())
                        .thenAccept(__ -> {
                            // lol
                            // we do everything possible to guarantee that this should be safe
                            // without doing a check here
                            //noinspection ConstantConditions,OptionalGetWithoutIsPresent
                            final Account account = getDatabase().getAccountByDiscordId(user.getId()).get();
                            final String profileUrl = System.getenv("DOMAIN") + "/profile/" + account.getId();
                            
                            final byte[] cardBytes = Renderer.generateRankCard(ctx.getGuild(), user, player);
                            final EmbedBuilder builder = new EmbedBuilder()
                                    .title("**" + user.getName() + "**'s rank card")
                                    .image("attachment://rank.png")
                                    .color(new Color(Renderer.PRIMARY_COLOUR))
                                    .description(String.format("[View full profile](%s)", profileUrl))
                                    .footer("You can change your background on your profile.", null);
                            getCatnip().rest().channel().deleteMessage(ctx.getChannel().getId(), message.id())
                                    .thenAccept(___ -> getCatnip().rest().channel()
                                            .sendMessage(ctx.getChannel().getId(),
                                                    new MessageOptions().addFile("rank.png", cardBytes)
                                                            .embed(builder.build()))
                                    );
                        }));
    }
    
    @Command(names = "profile", desc = "commands.levels.profile", usage = "profile [@mention]",
            examples = {"profile", "profile @someone"})
    public void profile(final CommandContext ctx) {
        final User user;
        final Player player;
        if(ctx.getMentions().isEmpty()) {
            user = ctx.getUser();
            player = ctx.getPlayer();
        } else {
            user = ctx.getMentions().get(0);
            player = getDatabase().getPlayer(user);
        }
        
        if(user.isBot()) {
            getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(), "Bots can't have profiles!");
            return;
        }
        
        getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                Emotes.LOADING_ICON + " Generating profile card (this will take a few seconds)")
                .thenAccept(message ->
                        getCatnip().rest().channel().triggerTypingIndicator(ctx.getChannel().getId())
                                .thenAccept(__ -> {
                                    // lol
                                    // we do everything possible to guarantee that this should be safe
                                    // without doing a check here
                                    //noinspection ConstantConditions,OptionalGetWithoutIsPresent
                                    final Account account = getDatabase().getAccountByDiscordId(user.getId()).get();
                                    final String profileUrl = System.getenv("DOMAIN") + "/profile/" + account.getId();
                                    
                                    final byte[] cardBytes = Renderer.generateProfileCard(user, player);
                                    final EmbedBuilder builder = new EmbedBuilder()
                                            .title("**" + user.getName() + "**'s profile card")
                                            .image("attachment://profile.png")
                                            .color(new Color(Renderer.PRIMARY_COLOUR))
                                            .description(String.format("[View full profile](%s)", profileUrl))
                                            .footer("You can change your description and background on your profile.", null);
                                    getCatnip().rest().channel().deleteMessage(ctx.getChannel().getId(), message.id())
                                            .thenApply(___ ->
                                                    getCatnip().rest().channel()
                                                            .sendMessage(ctx.getChannel().getId(),
                                                                    new MessageOptions().addFile("profile.png", cardBytes)
                                                                            .embed(builder.build()))
                                            );
                                }));
    }
    
    @Command(names = "score", desc = "commands.levels.score", usage = "score [@mention]",
            examples = {"score", "score @someone"})
    public void score(final CommandContext ctx) {
        final User user;
        final Player player;
        if(ctx.getMentions().isEmpty()) {
            user = ctx.getUser();
            player = ctx.getPlayer();
        } else {
            user = ctx.getMentions().get(0);
            player = getDatabase().getPlayer(user);
        }
        
        getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                String.format("**%s**'s score: **%s**", user.getName(), player.calculateScore()));
    }
    
    @Command(names = {"leaderboards", "ranks", "levels", "leaderboard", "rankings"}, desc = "commands.levels.leaderboards",
            usage = "leaderboards", examples = "leaderboards")
    public void ranks(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        final LevelsSettings settings = getMewna().getDatabase().getOrBaseSettings(LevelsSettings.class, guild.getId());
        if(!settings.isLevelsEnabled()) {
            getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(), "Levels are not enabled in this server. " +
                    "The server owner (or administrators) should enable them in the dashboard: <https://mewna.com/>")
            ;
            return;
        }
        getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                System.getenv("DOMAIN") + "/discord/leaderboards/" + guild.getId())
        ;
    }
    
    private long getXp(final Player player) {
        return 10 + getRandom().nextInt(10);
    }
}
