package com.mewna.plugin.plugins;

import com.mewna.Mewna;
import com.mewna.data.accounts.Account;
import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.util.ImageOptions;
import com.mewna.catnip.rest.guild.MemberData;
import com.mewna.catnip.shard.DiscordEvent.Raw;
import com.mewna.data.Player;
import com.mewna.event.discord.DiscordGuildMemberAdd;
import com.mewna.event.discord.DiscordMessageCreate;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.commands.Command;
import com.mewna.plugin.commands.CommandContext;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.PlayerEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemEventType;
import com.mewna.plugin.event.plugin.levels.LevelUpEvent;
import com.mewna.plugin.plugins.settings.LevelsSettings;
import com.mewna.plugin.util.Emotes;
import com.mewna.util.Templater;
import gg.amy.singyeong.QueryBuilder;
import io.sentry.Sentry;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.awt.*;
import java.sql.ResultSet;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mewna.util.MewnaFutures.block;
import static com.mewna.util.Translator.$;

/**
 * @author amy
 * @since 5/19/18.
 */
@SuppressWarnings({"unused", "WeakerAccess", "SqlResolve"})
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
        final var settings = block(database().getOrBaseSettings(LevelsSettings.class, guild.id()));
        mewna().statsClient().count("levelups", 1);
        
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
    }
    
    private void sendLevelUpMessage(final LevelsSettings settings, final LevelUpEvent event, final Member member) {
        if(settings.isLevelUpMessagesEnabled()) {
            final String message = map(event).render(settings.getLevelUpMessage());
            catnip().rest().channel().sendMessage(event.channel(), message);
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
    
    @Event(Raw.GUILD_MEMBER_ADD)
    public void handleJoinPersist(final DiscordGuildMemberAdd event) {
        final var player = block(database().getPlayer(event.user(), null));
        final var settings = block(database().getOrBaseSettings(LevelsSettings.class, event.guild().id()));
        final long xp = player.getXp(event.guild().id());
        final long level = xpToLevel(xp);
        // TODO: How to make this work right?
        removeAndAddRoleRewards(settings, event.guild(), event.member(), level, () -> {
        });
    }
    
    @Event(Raw.MESSAGE_CREATE)
    public void handleChatMessage(final DiscordMessageCreate event) {
        final User author = event.message().author();
        final var player = block(database().getPlayer(author, null));
        Account account;
        try {
            account = player.getAccount();
        } catch(final NoSuchElementException e) {
            mewna().accountManager().createNewDiscordLinkedAccount(player, author);
            account = player.getAccount();
        }
        if(account.banned()) {
            return;
        }
        final ImmutablePair<Boolean, Long> globalRes = mewna().ratelimiter()
                .checkUpdateRatelimit(author.id(), "chat-xp-global", TimeUnit.MINUTES.toMillis(10));
        if(!globalRes.left) {
            final long oldXp = player.getGlobalXp();
            final long xp = getXp(player);
            mewna().statsClient().count("xpgained.global", xp);
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
                                new PlayerEvent(SystemEventType.GLOBAL_LEVEL, player,
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
        final var settings = block(database().getOrBaseSettings(LevelsSettings.class, guild.id()));
        if(!settings.isLevelsEnabled()) {
            return;
        }
        final ImmutablePair<Boolean, Long> localRes = mewna().ratelimiter()
                .checkUpdateRatelimit(event.message().author().id(), "chat-xp-local:" + guild.id(),
                        TimeUnit.MINUTES.toMillis(1));
        if(!localRes.left) {
            final long oldXp = player.getXp(guild);
            final long xp = getXp(player);
            mewna().statsClient().count("xpgained.local", xp);
            player.incrementLocalXp(guild, xp);
            database().savePlayer(player).join();
            if(isLevelUp(oldXp, oldXp + xp)) {
                // Emit level-up event so we can process it
                mewna().pluginManager().processEvent(EventType.LEVEL_UP, new LevelUpEvent(guild, event.message().channelId(),
                        event.user(), event.member(), xpToLevel(oldXp + xp), oldXp + xp));
            }
        }
    }
    
    @Command(names = {"rank", "level"}, desc = "commands.levels.rank", usage = "rank [@mention]",
            examples = {"rank", "rank @someone"})
    public void rank(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        final var settings = block(database().getOrBaseSettings(LevelsSettings.class, guild.id()));
        if(!settings.isLevelsEnabled()) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.levels.not-enabled"));
            return;
        }
        final User user;
        final Player player;
        final boolean self;
        if(ctx.getMentions().isEmpty() || ctx.getMentions().get(0).id().equals(ctx.getUser().id())) {
            user = ctx.getUser();
            player = ctx.getPlayer();
            self = true;
        } else {
            user = ctx.getMentions().get(0);
            player = block(database().getPlayer(user, ctx.getProfiler()));
            self = false;
        }
        if(user.bot()) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.levels.bot"));
            return;
        }
        
        String generating;
        if(self) {
            generating = $(ctx.getLanguage(), "plugins.levels.commands.rank.generating.self");
        } else {
            generating = $(ctx.getLanguage(), "plugins.levels.commands.rank.generating.other")
                    .replace("$target", user.username());
        }
        generating = generating.replace("$mention", ctx.getUser().asMention());
        
        final var message = block(ctx.sendMessage(Emotes.LOADING_ICON + ' ' + generating));
        block(catnip().rest().channel().triggerTypingIndicator(ctx.getMessage().channelId()));
        
        try {
            // We do everything possible to guarantee that this should be safe
            // without doing a check here
            // TODO: Fix this for imported levels...
            // noinspection OptionalGetWithoutIsPresent
            final Account account = database().getAccountByDiscordId(user.id()).get();
            final String profileUrl = System.getenv("DOMAIN") + "/profile/" + account.id();
            
            final EmbedBuilder builder = new EmbedBuilder()
                    .title("**" + user.username() + "**'s rank card")
                    .image("attachment://rank.png")
                    .color(new Color(Mewna.PRIMARY_COLOUR))
                    .description('[' + $(ctx.getLanguage(), "plugins.levels.view-full-profile") + "](" + profileUrl + ')')
                    .footer($(ctx.getLanguage(), "plugins.levels.change-background"),
                            user.effectiveAvatarUrl());
            
            final Buffer response = block(mewna().singyeong()
                    .proxy(HttpMethod.POST, "/v1/render/rank", "renderer", new QueryBuilder().build(),
                            new JsonObject()
                                    .put("id", user.id())
                                    .put("background", account.customBackground())
                                    .put("avatarUrl", user.effectiveAvatarUrl(new ImageOptions().png().size(128)))
                                    .put("username", user.username())
                                    .put("exp", player.getXp(guild))
                                    .put("rank", getPlayerRankInGuild(guild, user))
                    ));
            final byte[] bytes = response.getBytes();
            
            block(catnip().rest().channel().deleteMessage(ctx.getMessage().channelId(), message.id()));
            catnip().rest().channel().sendMessage(ctx.getMessage().channelId(),
                    new MessageOptions()
                            .content(ctx.getUser().asMention())
                            .embed(builder.build())
                            .addFile("rank.png", bytes));
        } catch(final Exception e) {
            message.edit(Emotes.NO + ' ' + $(ctx.getLanguage(), "plugins.levels.render-error"));
            Sentry.capture(e);
            e.printStackTrace();
        }
    }
    
    @Command(names = "profile", desc = "commands.levels.profile", usage = "profile [@mention]",
            examples = {"profile", "profile @someone"})
    public void profile(final CommandContext ctx) {
        final User user;
        final Player player;
        final boolean self;
        if(ctx.getMentions().isEmpty() || ctx.getMentions().get(0).id().equals(ctx.getUser().id())) {
            user = ctx.getUser();
            player = ctx.getPlayer();
            self = true;
        } else {
            user = ctx.getMentions().get(0);
            player = block(database().getPlayer(user, ctx.getProfiler()));
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
        
        final var message = block(ctx.sendMessage(Emotes.LOADING_ICON + ' ' + finalGenerating));
        
        block(catnip().rest().channel().triggerTypingIndicator(ctx.getMessage().channelId()));
        try {
            // lol
            // we do everything possible to guarantee that this should be safe
            // without doing a check here
            // noinspection OptionalGetWithoutIsPresent
            final Account account = database().getAccountByDiscordId(user.id()).get();
            final String profileUrl = System.getenv("DOMAIN") + "/profile/" + account.id();
            final EmbedBuilder builder = new EmbedBuilder()
                    .title("**" + user.username() + "**'s profile card")
                    .image("attachment://profile.png")
                    .color(new Color(Mewna.PRIMARY_COLOUR))
                    .description('[' + $(ctx.getLanguage(), "plugins.levels.view-full-profile") + "](" + profileUrl + ')')
                    .footer($(ctx.getLanguage(), "plugins.levels.change-background-description"), null);
            
            final Buffer response = block(mewna().singyeong()
                    .proxy(HttpMethod.POST, "/v1/render/profile", "renderer", new QueryBuilder().build(),
                            new JsonObject()
                                    .put("id", user.id())
                                    .put("background", account.customBackground())
                                    .put("avatarUrl", user.effectiveAvatarUrl(new ImageOptions().png().size(128)))
                                    .put("displayName", account.displayName())
                                    .put("aboutText", account.aboutText())
                                    .put("exp", player.getGlobalXp())
                                    .put("rank", getPlayerRankGlobally(user))
                                    .put("score", player.calculateScore())
                    ));
            final byte[] bytes = response.getBytes();
            
            block(catnip().rest().channel().deleteMessage(ctx.getMessage().channelId(), message.id()));
            catnip().rest().channel().sendMessage(ctx.getMessage().channelId(),
                    new MessageOptions()
                            .content(ctx.getUser().asMention())
                            .addFile("profile.png", bytes)
                            .embed(builder.build()));
        } catch(final Exception e) {
            message.edit(Emotes.NO + ' ' + $(ctx.getLanguage(), "plugins.levels.render-error"));
            Sentry.capture(e);
            e.printStackTrace();
        }
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
            player = block(database().getPlayer(user, ctx.getProfiler()));
        }
        ctx.sendMessage($(ctx.getLanguage(), "plugins.levels.commands.score")
                .replace("$target", user.username())
                .replace("$score", player.calculateScore() + ""));
    }
    
    @Command(names = {"leaderboards", "ranks", "levels", "leaderboard", "rankings"}, desc = "commands.levels.leaderboards",
            usage = "leaderboards", examples = "leaderboards")
    public void ranks(final CommandContext ctx) {
        final Guild guild = ctx.getGuild();
        final var settings = block(database().getOrBaseSettings(LevelsSettings.class, guild.id()));
        if(!settings.isLevelsEnabled()) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.levels.not-enabled"));
            return;
        }
        ctx.sendMessage(System.getenv("DOMAIN") + "/discord/leaderboards/" + guild.id());
    }
    
    private long getXp(final Player player) {
        return 10 + random().nextInt(15);
    }
}
