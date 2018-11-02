package com.mewna.plugin.plugins;

import com.mewna.accounts.Account;
import com.mewna.catnip.entity.message.Message;
import com.mewna.data.DiscordCache;
import com.mewna.data.Player;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.plugins.settings.SecretSettings;
import com.mewna.plugin.util.Emotes;
import com.mewna.util.MewnaFutures;
import gg.amy.singyeong.QueryBuilder;
import io.vertx.core.json.JsonObject;

import java.util.Optional;

/**
 * @author amy
 * @since 10/18/18.
 */
@Plugin(name = "secret", desc = "spooky secret things :3", settings = SecretSettings.class, owner = true)
public class PluginSecret extends BasePlugin {
    @Command(names = "secret", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void secret(final CommandContext ctx) {
        catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "secret");
    }
    
    @Command(names = "inspect", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void debugInspect(final CommandContext ctx) {
        if(ctx.getArgs().size() != 2) {
            catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), Emotes.NO);
        } else {
            final String snowflake = ctx.getArgs().get(1)
                    .replaceAll("<@(!)?", "")
                    .replace(">", "");
            switch(ctx.getArgs().get(0).toLowerCase()) {
                case "player": {
                    final Optional<Player> optionalPlayer = database().getOptionalPlayer(snowflake);
                    if(optionalPlayer.isPresent()) {
                        final JsonObject o = JsonObject.mapFrom(optionalPlayer.get());
                        // TODO: ???
                        o.remove("account");
                        o.remove("votes");
                        o.remove("boxes");
                        final String json = o.encodePrettily();
                        catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "```Javascript\n" + json + "\n```");
                    } else {
                        catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), Emotes.NO);
                    }
                    break;
                }
                case "account": {
                    final Optional<Account> optionalAccount = mewna().accountManager().getAccountByLinkedDiscord(snowflake);
                    if(optionalAccount.isPresent()) {
                        final JsonObject o = JsonObject.mapFrom(optionalAccount.get());
                        o.remove("email");
                        final String json = o.encodePrettily();
                        catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "```Javascript\n" + json + "\n```");
                    } else {
                        catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), Emotes.NO);
                    }
                    break;
                }
                default: {
                    catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), Emotes.NO);
                    break;
                }
            }
        }
    }
    
    @Command(names = "guildcast", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void guildcast(final CommandContext ctx) {
        final String guildId = ctx.getGuild().id();
        mewna().singyeong().send("mewna-shard", new QueryBuilder().contains("guilds", guildId).build(),
                new JsonObject().put("henlo", guildId));
        catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "Casting guild " + guildId);
    }
    
    @Command(names = "vj", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void join(final CommandContext ctx) {
        // TODO: Cache accesses
        final String guildId = ctx.getGuild().id();
        mewna().singyeong().send("mewna-shard", new QueryBuilder().contains("guilds", guildId).build(),
                new JsonObject().put("type", "VOICE_JOIN").put("guild_id", guildId).put("channel_id", "436319633570529287"));
    }
    
    @Command(names = "vl", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void leave(final CommandContext ctx) {
        // TODO: Cache accesses
        final String guildId = ctx.getGuild().id();
        mewna().singyeong().send("mewna-shard", new QueryBuilder().contains("guilds", guildId).build(),
                new JsonObject().put("type", "VOICE_LEAVE").put("guild_id", guildId));
    }
    
    @Command(names = "vs", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void vs(final CommandContext ctx) {
        DiscordCache.voiceState(ctx.getGuild().id(), ctx.getUser().id())
                .thenAccept(vs -> catnip().rest().channel()
                        .sendMessage(ctx.getMessage().channelId(), "state: " + vs))
        .exceptionally(__ -> {
            catnip().rest().channel().sendMessage(ctx.getMessage().channelId(), "state: No state");
            return null;
        });
    }
    
    @Command(names = "play", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void play(final CommandContext ctx) {
        // TODO: Cache accesses
        final String guildId = ctx.getGuild().id();
        mewna().singyeong().send("nekomimi", new QueryBuilder().contains("guilds", guildId).build(),
                new JsonObject().put("type", "VOICE_PLAY").put("url", ctx.getArgstr()).put("guild_id", guildId));
    }
    
    @Command(names = "fetch", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void fetch(final CommandContext ctx) {
        final Message msg = ctx.getMessage();
        final String guildId = msg.guildId();
        final String channelId = msg.channelId();
        final String userId = msg.author().id();
        
        final var guild = DiscordCache.guild(guildId);
        final var user = DiscordCache.user(userId);
        final var channel = DiscordCache.textChannel(guildId, channelId);
        final var member = DiscordCache.member(guildId, userId);
        
        MewnaFutures.allOf(guild, user, channel, member).thenAccept(__ ->
                catnip().rest().channel()
                        .sendMessage(channelId, "" +
                                "__Results:__\n" +
                                "```\n" +
                                "  Guild: " + MewnaFutures.get(guild) + " \n" +
                                "   User: " + MewnaFutures.get(user) + " \n" +
                                "Channel: " + MewnaFutures.get(channel) + " \n" +
                                " Member: " + MewnaFutures.get(member) + " \n" +
                                "```"
                        ));
    }
}
