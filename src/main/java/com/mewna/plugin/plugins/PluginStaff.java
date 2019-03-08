package com.mewna.plugin.plugins;

import com.google.common.collect.ImmutableMap;
import com.mewna.data.Player;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.plugins.economy.Item;
import com.mewna.plugin.plugins.settings.*;
import com.mewna.plugin.util.Emotes;
import io.sentry.Sentry;
import io.vertx.core.json.JsonObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static com.mewna.util.Async.move;

/**
 * @author amy
 * @since 2/11/19.
 */
@Plugin(name = "staff", desc = "Staff-only commands~", settings = SecretSettings.class, staff = true)
public class PluginStaff extends BasePlugin {
    private final Map<String, Class<? extends PluginSettings>> configs = ImmutableMap.copyOf(new HashMap<>() {{
        put("behaviour", BehaviourSettings.class);
        put("economy", EconomySettings.class);
        put("emotes", EmotesSettings.class);
        put("levels", LevelsSettings.class);
        put("misc", MiscSettings.class);
        put("music", MusicSettings.class);
        put("twitch", TwitchSettings.class);
        put("welcoming", WelcomingSettings.class);
    }});
    
    @Command(names = "config", desc = "staff-only", usage = "staff-only", examples = "staff-only", staff = true)
    public void config(final CommandContext ctx) {
        if(ctx.getArgs().isEmpty()) {
            ctx.sendMessage(Emotes.NO + " You need to provide a server id and a config type " +
                    "(ex. `mew.config 1234567890 behaviour`), " +
                    "or do `mew.config types` for all config types.");
        } else if(ctx.getArgs().get(0).equalsIgnoreCase("types")) {
            ctx.sendMessage("```CSS\n" +
                    "[behaviour] prefix and similar\n" +
                    "  [economy] currency symbol and commands\n" +
                    "   [emotes] commands\n" +
                    "   [levels] level-up message, role rewards, and commands\n" +
                    "     [misc] anything that doesn't fit elsewhere\n" +
                    "    [music] commands\n" +
                    "   [twitch] streamers and webhook channel\n" +
                    "[welcoming] messages and channel\n" +
                    "```");
        } else if(ctx.getArgs().size() == 2 && ctx.getArgs().get(0).matches("\\d+")
                && configs.containsKey(ctx.getArgs().get(1).toLowerCase())) {
            database().getOrBaseSettings(ctx.getArgs().get(1).toLowerCase(), ctx.getArgs().get(0)).thenAccept(settings -> {
                final JsonObject json = JsonObject.mapFrom(settings);
                final Iterable<String> split = new ArrayList<>(Arrays.asList(json.encodePrettily().split("\n")));
                final Collection<String> pages = new ArrayList<>();
                String page = "```Javascript\n";
                for(final String s : split) {
                    //noinspection StringConcatenationInLoop
                    page += s + '\n';
                    if(page.length() > 1500) {
                        page += "```";
                        pages.add("" + page);
                        page = "```Javascript\n";
                    }
                }
                if(!page.isEmpty()) {
                    if(!page.endsWith("```")) {
                        page += "```";
                    }
                    if(!page.startsWith("```")) {
                        page = "```Javascript\n" + page;
                    }
                    pages.add("" + page);
                }
                move(() -> {
                    for(final String p : pages) {
                        // lol joins :^^^^^^^)
                        ctx.getUser().createDM().toCompletableFuture().join().sendMessage(p).toCompletableFuture().join();
                        try {
                            Thread.sleep(100L);
                        } catch(final InterruptedException ignored) {
                        }
                    }
                    ctx.sendMessage("Check your DMs ^^");
                });
            });
        } else {
            ctx.sendMessage(Emotes.NO + " You need to provide a server id and a config type " +
                    "(ex. `mew.config 1234567890 behaviour`), " +
                    "or do `mew.config types` for all config types.");
        }
    }
    
    @Command(names = "trace", desc = "staff-only", usage = "staff-only", examples = "staff-only", staff = true)
    public void trace(final CommandContext ctx) {
        ctx.getProfiler().end();
        final StringBuilder sb = new StringBuilder("```CSS\n");
        ctx.getProfiler().sections().forEach(section -> sb.append('[').append(section.name()).append("] ")
                .append(section.end() - section.start()).append("ms\n"));
        sb.append('\n');
        try {
            sb.append("[worker] ").append(InetAddress.getLocalHost().getHostName()).append('\n');
        } catch(final UnknownHostException e) {
            sb.append("[worker] unknown (check sentry)\n");
            Sentry.capture(e);
        }
        // sb.append("[shard] ").append(0).append('\n');
        sb.append("[guild] ").append(ctx.getGuild().id()).append('\n');
        sb.append("[user] ").append(ctx.getUser().id()).append('\n');
        sb.append("[message] ").append(ctx.getMessage().id()).append('\n');
        sb.append("[ts] ").append(ctx.getSource().timestamp()).append('\n');
        sb.append("[sender] ").append(ctx.getSource().sender()).append('\n');
        
        sb.append("```");
        
        ctx.sendMessage(sb.toString());
    }
    
    @Command(names = "grant", desc = "secret", usage = "sercet", examples = "secret", staff = true)
    public void grant(final CommandContext ctx) {
        if(ctx.getArgs().size() < 3) {
            ctx.sendMessage("```CSS\n" +
                    "[ITEMS] grant item <user id> <item> <amount>\n" +
                    "[GUILD EXP] grant exp <user id> <guild id> <amount>\n" +
                    "[GUILD EXP] grant levels <user id> <guild id> <amount>\n" +
                    "[MONEY] grant money <user id> <amount>\n" +
                    "```");
        } else {
            final String mode = ctx.getArgs().remove(0).toLowerCase();
            final String playerId = ctx.getArgs().get(0).replace("<@", "").replace(">", "");
            switch(mode) {
                case "item": {
                    final Optional<Item> maybeItem = Arrays.stream(Item.values())
                            .filter(e -> e.getName().equalsIgnoreCase(ctx.getArgs().get(1)))
                            .findFirst();
                    int amount = 1;
                    if(ctx.getArgs().size() > 2) {
                        try {
                            amount = Integer.parseInt(ctx.getArgs().get(2));
                        } catch(final Exception e) {
                            ctx.sendMessage(Emotes.NO + " Invalid amount");
                            return;
                        }
                    }
                    if(maybeItem.isPresent()) {
                        final Item item = maybeItem.get();
                        final int finalAmount = amount;
                        database().getOptionalPlayer(playerId, ctx.getProfiler()).thenAccept(o -> move(() -> {
                            if(o.isPresent()) {
                                final Player player = o.get();
                                player.addAllToInventory(ImmutableMap.of(item, (long) finalAmount));
                                database().savePlayer(player).thenAccept(__ -> ctx.sendMessage(Emotes.YES));
                            } else {
                                ctx.sendMessage(Emotes.NO + " No such player!");
                            }
                        }));
                    } else {
                        ctx.sendMessage(Emotes.NO + " No such item!");
                    }
                    break;
                }
                case "exp": {
                    try {
                        final String guildId = ctx.getArgs().get(1);
                        final long amount = Long.parseLong(ctx.getArgs().get(2));
                        database().getOptionalPlayer(playerId, ctx.getProfiler()).thenAccept(o -> move(() -> {
                            if(o.isPresent()) {
                                final Player player = o.get();
                                player.setLocalXp(guildId, amount);
                                database().savePlayer(player).thenAccept(__ -> ctx.sendMessage(Emotes.YES));
                            } else {
                                ctx.sendMessage(Emotes.NO + " No such player!");
                            }
                        }));
                    } catch(final Exception e) {
                        ctx.sendMessage(Emotes.NO + " Invalid command usage!");
                    }
                    break;
                }
                case "levels": {
                    try {
                        final String guildId = ctx.getArgs().get(1);
                        final int level = Integer.parseInt(ctx.getArgs().get(2));
                        database().getOptionalPlayer(playerId, ctx.getProfiler()).thenAccept(o -> move(() -> {
                            if(o.isPresent()) {
                                final Player player = o.get();
                                player.setLocalXp(guildId, PluginLevels.fullLevelToXp(level));
                                database().savePlayer(player).thenAccept(__ -> ctx.sendMessage(Emotes.YES));
                            } else {
                                ctx.sendMessage(Emotes.NO + " No such player!");
                            }
                        }));
                    } catch(final Exception e) {
                        ctx.sendMessage(Emotes.NO + " Invalid command usage!");
                    }
                    break;
                }
                case "money": {
                    try {
                        final long amount = Long.parseLong(ctx.getArgs().get(1));
                        database().getOptionalPlayer(playerId, ctx.getProfiler()).thenAccept(o -> move(() -> {
                            if(o.isPresent()) {
                                final Player player = o.get();
                                player.setBalance(amount);
                                database().savePlayer(player).thenAccept(__ -> ctx.sendMessage(Emotes.YES));
                            } else {
                                ctx.sendMessage(Emotes.NO + " No such player!");
                            }
                        }));
                    } catch(final Exception e) {
                        ctx.sendMessage(Emotes.NO + " Invalid command usage!");
                    }
                    break;
                }
                default: {
                    ctx.sendMessage(Emotes.NO);
                }
            }
        }
    }
}
