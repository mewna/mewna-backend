package com.mewna.plugin.plugins;

import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.rest.guild.MemberData;
import com.mewna.catnip.shard.DiscordEvent.Raw;
import com.mewna.catnip.util.SafeVertxCompletableFuture;
import com.mewna.data.Player;
import com.mewna.event.discord.DiscordMessageCreate;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.PlayerEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemUserEventType;
import com.mewna.plugin.event.plugin.levels.LevelUpEvent;
import com.mewna.plugin.plugins.settings.LevelsSettings;
import com.mewna.plugin.util.Emotes;
import com.mewna.plugin.util.Renderer;
import com.mewna.util.Templater;
import io.sentry.Sentry;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.awt.*;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mewna.util.Async.move;
import static com.mewna.util.Translator.$;

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
        Mewna.getInstance().database().getStore().sql("SELECT COUNT(*) AS count FROM players WHERE data->'guildXp'->'"
                + guild.id() + "' IS NOT NULL;", p -> {
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
        final String guildId = guild.id();
        final String playerId = player.id();
        Mewna.getInstance().database().getStore().sql("SELECT rank FROM (SELECT row_number() OVER (" +
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
        final String playerId = player.id();
        Mewna.getInstance().database().getStore().sql("SELECT rank FROM (SELECT row_number() OVER (" +
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
        try {
            final Guild guild = event.guild();
            final User user = event.user();
            final Map<String, String> data = new HashMap<>();
            final JsonObject jGuild = JsonObject.mapFrom(guild);
            for(final String key : jGuild.fieldNames()) {
                data.put("server." + key, jGuild.getMap().get(key) + "");
            }
            final JsonObject jUser = JsonObject.mapFrom(user);
            for(final String key : jUser.fieldNames()) {
                data.put("user." + key, jUser.getMap().get(key) + "");
            }
            data.put("user.name", user.username());
            data.put("user.mention", user.asMention());
            data.put("level", event.level() + "");
            data.put("xp", event.xp() + "");
            return Templater.fromMap(data);
        } catch(final Exception e) {
            Sentry.capture(e);
            throw new RuntimeException(e);
        }
    }
    
    @Event(EventType.LEVEL_UP)
    public void handleLevelUp(final LevelUpEvent event) {
        final Guild guild = event.guild();
        mewna().statsClient().count("discord.backend.levelups", 1);
        database().getOrBaseSettings(LevelsSettings.class, guild.id()).thenAccept(settings -> {
            if(settings.isLevelsEnabled()) {
                final Member member = event.member();
                if(settings.isRemovePreviousRoleRewards()) {
                    removeAndAddRoleRewards(settings, guild, member, event.level(), () -> {
                    });
                } else {
                    addRoleRewards(settings, guild, member, event.level(), () -> {
                    });
                }
                sendLevelUpMessage(settings, event, member);
            }
        });
    }
    
    private void sendLevelUpMessage(final LevelsSettings settings, final LevelUpEvent event, final Member member) {
        if(settings.isLevelUpMessagesEnabled()) {
            final String message = map(event).render(settings.getLevelUpMessage());
            catnip().rest().channel().sendMessage(event.channel(), message).exceptionally(e -> {
                Sentry.capture(e);
                return null;
            });
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
            final Collection<String> endRoles = new HashSet<>(member.roleIds());
            endRoles.addAll(rewards);
            endRoles.removeAll(removeRoles);
            final MemberData memberData = new MemberData();
            //noinspection ResultOfMethodCallIgnored
            endRoles.forEach(memberData::addRole);
            catnip().rest().guild().modifyGuildMember(guild.id(), member.id(), memberData)
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
            member.roleIds().forEach(memberData::addRole);
            rewards.forEach(memberData::addRole);
            catnip().rest().guild().modifyGuildMember(guild.id(), member.id(), memberData)
                    .thenAccept(__ -> callback.run());
        }
    }
    
    @Event(Raw.MESSAGE_CREATE)
    public void handleChatMessage(final DiscordMessageCreate event) {
        final User author = event.message().author();
        database().getPlayer(author, null).thenAccept(player -> {
            move(() -> {
                final ImmutablePair<Boolean, Long> globalRes = mewna().ratelimiter()
                        .checkUpdateRatelimit(author.id(), "chat-xp-global", TimeUnit.MINUTES.toMillis(10));
                if(!globalRes.left) {
                    final long oldXp = player.getGlobalXp();
                    final long xp = getXp(player);
                    mewna().statsClient().count("discord.backend.xpgained.global", xp);
                    player.incrementGlobalXp(getXp(player));
                    database().savePlayer(player).join();
                    // Level-up notifications here?
                    if(isLevelUp(oldXp, oldXp + xp)) {
                        final long level = xpToLevel(oldXp + xp);
                        // lol
                        switch((int) level) {
                            case 10:
                            case 25:
                            case 50:
                            case 100: {
                                mewna().pluginManager().processEvent(EventType.PLAYER_EVENT,
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
    
                final Guild guild = event.guild();
                database().getOrBaseSettings(LevelsSettings.class, guild.id()).thenAccept(settings -> {
                    if(!settings.isLevelsEnabled()) {
                        return;
                    }
                    final ImmutablePair<Boolean, Long> localRes = mewna().ratelimiter()
                            .checkUpdateRatelimit(event.message().author().id(), "chat-xp-local:" + guild.id(),
                                    TimeUnit.MINUTES.toMillis(1));
                    if(!localRes.left) {
                        final long oldXp = player.getXp(guild);
                        final long xp = getXp(player);
                        mewna().statsClient().count("discord.backend.xpgained.local", xp);
                        player.incrementLocalXp(guild, xp);
                        database().savePlayer(player).join();
                        if(isLevelUp(oldXp, oldXp + xp)) {
                            // Emit level-up event so we can process it
                            mewna().pluginManager().processEvent(EventType.LEVEL_UP, new LevelUpEvent(guild, event.message().channelId(),
                                    event.user(), event.member(), xpToLevel(oldXp + xp), oldXp + xp));
                        }
                    }
                });
            });
        });
    }
    
    @Command(names = {"rank", "level"}, desc = "commands.levels.rank", usage = "rank [@mention]",
            examples = {"rank", "rank @someone"})
    public void rank(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        database().getOrBaseSettings(LevelsSettings.class, guild.id()).thenAccept(settings -> {
            if(!settings.isLevelsEnabled()) {
                ctx.sendMessage($(ctx.getLanguage(), "plugins.levels.not-enabled"));
                return;
            }
            final User user;
            final CompletableFuture<Player> playerFuture;
            final boolean self;
            if(ctx.getMentions().isEmpty() || ctx.getMentions().get(0).id().equals(ctx.getUser().id())) {
                user = ctx.getUser();
                playerFuture = SafeVertxCompletableFuture.completedFuture(ctx.getPlayer());
                self = true;
            } else {
                user = ctx.getMentions().get(0);
                playerFuture = database().getPlayer(user, ctx.getProfiler());
                self = false;
            }
            if(user.bot()) {
                ctx.sendMessage($(ctx.getLanguage(), "plugins.levels.bot"));
                return;
            }
            playerFuture.thenAccept(player -> {
                String generating;
                if(self) {
                    generating = $(ctx.getLanguage(), "plugins.levels.commands.rank.generating.self");
                } else {
                    generating = $(ctx.getLanguage(), "plugins.levels.commands.rank.generating.other")
                            .replace("$target", user.username());
                }
                generating = generating.replace("$mention", ctx.getUser().asMention());
                
                ctx.sendMessage(
                        Emotes.LOADING_ICON + ' ' + generating)
                        .thenAccept(message -> catnip().rest().channel().triggerTypingIndicator(ctx.getMessage().channelId())
                                .thenAccept(__ -> move(() -> {
                                    try {
                                        // lol
                                        // we do everything possible to guarantee that this should be safe
                                        // without doing a check here
                                        //noinspection ConstantConditions,OptionalGetWithoutIsPresent
                                        final Account account = database().getAccountByDiscordId(user.id()).get();
                                        final String profileUrl = System.getenv("DOMAIN") + "/profile/" + account.id();
                                        
                                        final byte[] cardBytes = Renderer.generateRankCard(ctx.getGuild(), user, player);
                                        final EmbedBuilder builder = new EmbedBuilder()
                                                .title("**" + user.username() + "**'s rank card")
                                                .image("attachment://rank.png")
                                                .color(new Color(Renderer.PRIMARY_COLOUR))
                                                .description('[' + $(ctx.getLanguage(), "plugins.levels.view-full-profile") + "](" + profileUrl + ')')
                                                .footer($(ctx.getLanguage(), "plugins.levels.change-background"),
                                                        user.effectiveAvatarUrl());
                                        catnip().rest().channel().deleteMessage(ctx.getMessage().channelId(), message.id())
                                                .thenAccept(___ -> catnip().rest().channel()
                                                        .sendMessage(ctx.getMessage().channelId(),
                                                                new MessageOptions()
                                                                        .content(ctx.getUser().asMention())
                                                                        .embed(builder.build())
                                                                        .addFile("rank.png", cardBytes))
                                                        .exceptionally(e -> {
                                                            e.printStackTrace();
                                                            return null;
                                                        })
                                                );
                                    } catch(final Exception e) {
                                        Sentry.capture(e);
                                        e.printStackTrace();
                                    }
                                })).exceptionally(e -> {
                                    e.printStackTrace();
                                    return null;
                                }));
            });
        });
    }
    
    @Command(names = "profile", desc = "commands.levels.profile", usage = "profile [@mention]",
            examples = {"profile", "profile @someone"})
    public void profile(final CommandContext ctx) {
        final User user;
        final CompletableFuture<Player> playerFuture;
        final boolean self;
        if(ctx.getMentions().isEmpty() || ctx.getMentions().get(0).id().equals(ctx.getUser().id())) {
            user = ctx.getUser();
            playerFuture = SafeVertxCompletableFuture.completedFuture(ctx.getPlayer());
            self = true;
        } else {
            user = ctx.getMentions().get(0);
            playerFuture = database().getPlayer(user, ctx.getProfiler());
            self = false;
        }
        
        String generating;
        if(self) {
            generating = $(ctx.getLanguage(), "plugins.levels.commands.profile.generating.self");
        } else {
            generating = $(ctx.getLanguage(), "plugins.levels.commands.profile.generating.other")
                    .replace("$target", user.username());
        }
        generating = generating.replace("$mention", ctx.getUser().asMention());
        final String finalGenerating = generating;
        
        //noinspection CodeBlock2Expr
        playerFuture.thenAccept(player -> {
            ctx.sendMessage(
                    Emotes.LOADING_ICON + ' ' + finalGenerating)
                    .thenAccept(message ->
                            catnip().rest().channel().triggerTypingIndicator(ctx.getMessage().channelId())
                                    .thenAccept(__ -> move(() -> {
                                        // lol
                                        // we do everything possible to guarantee that this should be safe
                                        // without doing a check here
                                        //noinspection ConstantConditions,OptionalGetWithoutIsPresent
                                        final Account account = database().getAccountByDiscordId(user.id()).get();
                                        final String profileUrl = System.getenv("DOMAIN") + "/profile/" + account.id();
                                        final byte[] cardBytes = Renderer.generateProfileCard(user, player);
                                        final EmbedBuilder builder = new EmbedBuilder()
                                                .title("**" + user.username() + "**'s profile card")
                                                .image("attachment://profile.png")
                                                .color(new Color(Renderer.PRIMARY_COLOUR))
                                                .description('[' + $(ctx.getLanguage(), "plugins.levels.view-full-profile") + "](" + profileUrl + ')')
                                                .footer($(ctx.getLanguage(), "plugins.levels.change-background-description"), null);
                                        catnip().rest().channel().deleteMessage(ctx.getMessage().channelId(), message.id())
                                                .thenApply(___ ->
                                                        catnip().rest().channel()
                                                                .sendMessage(ctx.getMessage().channelId(),
                                                                        new MessageOptions()
                                                                                .content(ctx.getUser().asMention())
                                                                                .addFile("profile.png", cardBytes)
                                                                                .embed(builder.build()))
                                                );
                                    })));
        });
    }
    
    @Command(names = "score", desc = "commands.levels.score", usage = "score [@mention]",
            examples = {"score", "score @someone"})
    public void score(final CommandContext ctx) {
        final User user;
        final CompletableFuture<Player> playerFuture;
        if(ctx.getMentions().isEmpty()) {
            user = ctx.getUser();
            playerFuture = SafeVertxCompletableFuture.completedFuture(ctx.getPlayer());
        } else {
            user = ctx.getMentions().get(0);
            playerFuture = database().getPlayer(user, ctx.getProfiler());
        }
        playerFuture.thenAccept(player -> ctx.sendMessage(
                $(ctx.getLanguage(), "plugins.levels.commands.score")
                        .replace("$target", user.username())
                        .replace("$score", player.calculateScore() + "")));
    }
    
    @Command(names = {"leaderboards", "ranks", "levels", "leaderboard", "rankings"}, desc = "commands.levels.leaderboards",
            usage = "leaderboards", examples = "leaderboards")
    public void ranks(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        database().getOrBaseSettings(LevelsSettings.class, guild.id()).thenAccept(settings -> {
            if(!settings.isLevelsEnabled()) {
                ctx.sendMessage($(ctx.getLanguage(), "plugins.levels.not-enabled"));
                return;
            }
            ctx.sendMessage(
                    System.getenv("DOMAIN") + "/discord/leaderboards/" + guild.id());
        });
    }
    
    private long getXp(final Player player) {
        return 10 + random().nextInt(15);
    }
}
