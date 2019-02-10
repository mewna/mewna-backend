package com.mewna.plugin.plugins;

import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.catnip.entity.message.Message;
import com.mewna.data.DiscordCache;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.plugins.settings.SecretSettings;
import com.mewna.plugin.plugins.settings.TwitchSettings;
import com.mewna.plugin.util.Emotes;
import com.mewna.util.MewnaFutures;
import gg.amy.singyeong.QueryBuilder;
import io.sentry.Sentry;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author amy
 * @since 10/18/18.
 */
@Plugin(name = "secret", desc = "spooky secret things :3", settings = SecretSettings.class, owner = true)
public class PluginSecret extends BasePlugin {
    @Command(names = "secret", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void secret(final CommandContext ctx) {
        ctx.sendMessage("secret");
    }
    
    @Command(names = "inspect", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void debugInspect(final CommandContext ctx) {
        if(ctx.getArgs().size() != 2) {
            ctx.sendMessage(Emotes.NO);
        } else {
            final String snowflake = ctx.getArgs().get(1)
                    .replaceAll("<@(!)?", "")
                    .replace(">", "");
            switch(ctx.getArgs().get(0).toLowerCase()) {
                case "player": {
                    database().getOptionalPlayer(snowflake).thenAccept(optionalPlayer -> {
                        if(optionalPlayer.isPresent()) {
                            final JsonObject o = JsonObject.mapFrom(optionalPlayer.get());
                            // TODO: ???
                            o.remove("account");
                            o.remove("votes");
                            o.remove("boxes");
                            final String json = o.encodePrettily();
                            ctx.sendMessage("```Javascript\n" + json + "\n```");
                        } else {
                            ctx.sendMessage(Emotes.NO);
                        }
                    });
                    break;
                }
                case "account": {
                    final Optional<Account> optionalAccount = mewna().accountManager().getAccountByLinkedDiscord(snowflake);
                    if(optionalAccount.isPresent()) {
                        final JsonObject o = JsonObject.mapFrom(optionalAccount.get());
                        o.remove("email");
                        final String json = o.encodePrettily();
                        ctx.sendMessage("```Javascript\n" + json + "\n```");
                    } else {
                        ctx.sendMessage(Emotes.NO);
                    }
                    break;
                }
                case "guild": {
                
                }
                default: {
                    ctx.sendMessage(Emotes.NO);
                    break;
                }
            }
        }
    }
    
    @Command(names = "guildcast", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void guildcast(final CommandContext ctx) {
        final String guildId = ctx.getGuild().id();
        mewna().singyeong().send("shards", new QueryBuilder().contains("guilds", guildId).build(),
                new JsonObject().put("henlo", guildId));
        ctx.sendMessage("Casting guild " + guildId);
    }
    
    @Command(names = "guildcheck", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void guildcheck(final CommandContext ctx) {
        final String guildId = ctx.getGuild().id();
        
        //noinspection CodeBlock2Expr
        DiscordCache.guild(guildId).thenAccept(g -> {
            ctx.sendMessage("Checked casted guild " + guildId + " with result " + g);
        }).exceptionally(e -> {
            ctx.sendMessage("Checked casted guild " + guildId + " with exception");
            e.printStackTrace();
            return null;
        });
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
    
    @Command(names = "dm", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void dm(final CommandContext ctx) {
        catnip().rest().user().createDM(ctx.getUser().id()).thenAccept(channel -> channel.sendMessage("test!"));
    }
    
    @Command(names = "forcetwitchresub", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void meme(final CommandContext ctx) {
        new Thread(() -> {
            final Set<String> ids = new HashSet<>();
            database().getStore().sql("SELECT data FROM settings_twitch;", c -> {
                final ResultSet rs = c.executeQuery();
                while(rs.next()) {
                    try {
                        final TwitchSettings s = Json.mapper.readValue(rs.getString("data"), TwitchSettings.class);
                        s.getTwitchStreamers().stream()
                                .filter(e -> e.isStreamStartMessagesEnabled() || e.isStreamEndMessagesEnabled())
                                .forEach(e -> ids.add(e.getId()));
                    } catch(final IOException e) {
                        Sentry.capture(e);
                    }
                }
            });
            ids.forEach(e -> Mewna.getInstance().singyeong().send("telepathy",
                    new QueryBuilder().build(),
                    new JsonObject().put("t", "TWITCH_SUBSCRIBE")
                            .put("d", new JsonObject().put("id", e).put("topic", "streams"))));
            catnip().rest().user().createDM(ctx.getUser().id())
                    .thenAccept(channel -> channel.sendMessage("Finished forcing twitch resub (" + ids.size() + " streamers)."));
        }).start();
    }
}
