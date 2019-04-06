package com.mewna.plugin.plugins;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mewna.catnip.entity.builder.EmbedBuilder;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.util.SafeVertxCompletableFuture;
import com.mewna.data.DiscordCache;
import com.mewna.data.Player;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.metadata.Payment;
import com.mewna.plugin.metadata.Ratelimit;
import com.mewna.plugin.plugins.economy.Box;
import com.mewna.plugin.plugins.economy.Item;
import com.mewna.plugin.plugins.economy.LootTables;
import com.mewna.plugin.plugins.settings.EconomySettings;
import com.mewna.plugin.util.CurrencyHelper;
import com.mewna.util.Time;
import io.sentry.Sentry;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.ToString;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.mewna.data.Player.MAX_INV_WEIGHT;
import static com.mewna.plugin.plugins.PluginEconomy.ReelSymbol.*;
import static com.mewna.util.Translator.$;

/**
 * @author amy
 * @since 5/19/18.
 */
@SuppressWarnings("unused")
@Plugin(name = "Economy", desc = "Let people earn and spend money.", settings = EconomySettings.class)
public class PluginEconomy extends BasePlugin {
    public static final int VOTE_BONUS = 750;
    private static final long SLOTS_BASE_COST = 10;
    private static final long GAMBLE_BASE_COST = 25;
    private static final long HEIST_BASE_COST = 300;
    private static final long CRIME_BASE_COST = 10;
    private static final long DAILY_BASE_REWARD = 100;
    private static final int GAMBLE_WUMPUS_COUNT = 4;
    private final Map<String, SlotMachine> slotsCache = new HashMap<>();
    @Inject
    private CurrencyHelper helper;
    
    @Ratelimit(time = 5)
    @Command(names = {"balance", "bal"}, desc = "commands.economy.balance", usage = "balance [player]",
            examples = {"balance", "balance @someone"})
    public void balance(final CommandContext ctx) {
        if(ctx.getMessage().mentionedUsers().isEmpty()) {
            final Player player = ctx.getPlayer();
            final long balance = player.getBalance();
            ctx.sendMessage($(ctx.getLanguage(), "plugins.economy.commands.balance.self")
                    .replace("$amount", balance + "")
                    .replace("$symbol", helper.getCurrencySymbol(ctx)));
        } else {
            final User m = ctx.getMessage().mentionedUsers().get(0);
            database().getPlayer(m, ctx.getProfiler()).thenAccept(player -> {
                final long balance = player.getBalance();
                ctx.sendMessage($(ctx.getLanguage(), "plugins.economy.commands.balance.other")
                        .replace("$target", m.username())
                        .replace("$amount", "" + balance)
                        .replace("$symbol", helper.getCurrencySymbol(ctx)));
            });
        }
    }
    
    @Ratelimit(time = 5)
    @Command(names = "pay", desc = "commands.economy.pay", usage = "pay <mention> <amount>",
            examples = "pay @someone 100")
    public void pay(final CommandContext ctx) {
        if(ctx.getMentions().isEmpty()) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.economy.commands.pay.needs-mention"));
            return;
        }
        if(ctx.getArgs().size() < 2) {
            ctx.sendMessage($(ctx.getLanguage(), "plugins.economy.commands.pay.needs-mention-amount"));
            return;
        }
        final Player sender = ctx.getPlayer();
        database().getPlayer(ctx.getMentions().get(0), ctx.getProfiler()).thenAccept(target -> {
            if(target.getId().equalsIgnoreCase(sender.getId())) {
                ctx.sendMessage($(ctx.getLanguage(), "plugins.economy.commands.pay.no-self-pay"));
                return;
            }
            final ImmutablePair<Boolean, Long> res = helper.handlePayment(ctx, ctx.getArgs().get(1), 1, Long.MAX_VALUE);
            if(res.left) {
                target.incrementBalance(res.right);
                database().savePlayer(target);
                
                ctx.sendMessage($(ctx.getLanguage(), "plugins.economy.commands.pay.success")
                        .replace("$amount", "" + res.right)
                        .replace("$symbol", helper.getCurrencySymbol(ctx))
                        .replace("$target", ctx.getMentions().get(0).username())
                        .replace("$user", ctx.getUser().username()));
            }
        });
    }
    
    @Ratelimit(time = 5)
    @Command(names = "daily", desc = "commands.economy.daily", usage = "daily", examples = "daily")
    public void daily(final CommandContext ctx) {
        final Player player = ctx.getPlayer();
        final Guild guild = ctx.getGuild();
        final ZoneId zone = ZoneId.systemDefault();
        final LocalDateTime last = Instant.ofEpochMilli(player.getLastDaily()).atZone(zone).toLocalDateTime();
        final LocalDateTime now = LocalDateTime.now();
        // Prevent doing too fast
        if(last.toLocalDate().plusDays(1).toEpochDay() > now.toLocalDate().toEpochDay()) {
            final long nextMillis = TimeUnit.SECONDS.toMillis(last.toLocalDate().plusDays(1).atStartOfDay(zone).toEpochSecond());
            final long nowMillis = TimeUnit.SECONDS.toMillis(now.toEpochSecond(zone.getRules().getOffset(now)));
            ctx.sendMessage(
                    new MessageOptions()
                            .content($(ctx.getLanguage(), "plugins.economy.commands.daily.failure-wait")
                                    .replace("$time", Time.toHumanReadableDuration(nextMillis - nowMillis)))
                            .embed(new EmbedBuilder()
                                    .title("Hint")
                                    .description($(ctx.getLanguage(), "plugins.economy.vote-nag")
                                            .replace("$link", "https://discordbots.org/bot/251930037673132032/vote")
                                            .replace("$amount", VOTE_BONUS + "")
                                            .replace("$symbol", helper.getCurrencySymbol(ctx)))
                                    .build()));
            return;
        }
        tryDropBox(ctx).thenAccept(__ -> {
            final boolean streak;
            // check streak
            streak = last.toLocalDate().plusDays(2).toEpochDay() >= now.toLocalDate().toEpochDay();
            
            String msg = $(ctx.getLanguage(), "plugins.economy.commands.daily.collect-base")
                    .replace("$amount", "" + DAILY_BASE_REWARD)
                    .replace("$symbol", helper.getCurrencySymbol(ctx))
                    + "\n\n";
            
            if(streak) {
                player.incrementDailyStreak();
                final long bonus = 100 + 15 * (player.getDailyStreak() - 1);
                player.incrementBalance(DAILY_BASE_REWARD + bonus);
                msg += $(ctx.getLanguage(), "plugins.economy.commands.daily.collect-streak")
                        .replace("$amount", "" + bonus)
                        .replace("$symbol", helper.getCurrencySymbol(ctx))
                        .replace("$streak", "" + player.getDailyStreak());
            } else {
                player.resetDailyStreak();
                player.incrementBalance(DAILY_BASE_REWARD);
                msg += $(ctx.getLanguage(), "plugins.economy.commands.daily.streak-break")
                        .replace("$symbol", helper.getCurrencySymbol(ctx));
            }
            
            player.updateLastDaily();
            final String finalMsg = msg;
            database().savePlayer(player).thenAccept(___ -> ctx.sendMessage(
                    new MessageOptions()
                            .content(finalMsg)
                            .embed(new EmbedBuilder()
                                    .title("Hint")
                                    .description($(ctx.getLanguage(), "plugins.economy.vote-nag")
                                            .replace("$link", "https://discordbots.org/bot/251930037673132032/vote")
                                            .replace("$amount", VOTE_BONUS + "")
                                            .replace("$symbol", helper.getCurrencySymbol(ctx)))
                                    .build())));
        });
    }
    
    @Ratelimit(time = 20)
    @Command(names = "crime", desc = "commands.economy.crime", usage = "crime", examples = "crime")
    public void crime(final CommandContext ctx) {
        final int choice = random().nextInt(10) + 1;
        final int amount = random().nextInt(Math.toIntExact(CRIME_BASE_COST)) + 1;
        final String text = $(ctx.getLanguage(), "plugins.economy.commands.crime." + choice)
                .replace("$amount", "" + amount)
                .replace("$symbol", helper.getCurrencySymbol(ctx));
        
        ctx.getPlayer().incrementBalance(amount);
        database().savePlayer(ctx.getPlayer())
                .thenAccept(__ -> ctx.sendMessage(text)
                        .thenAccept(___ -> tryDropBox(ctx)));
    }
    
    @Payment(min = HEIST_BASE_COST)
    @Ratelimit(time = 5 * 60)
    @Command(names = "heist", desc = "commands.economy.heist", usage = "heist", examples = "heist")
    public void heist(final CommandContext ctx) {
        final int chance = random().nextInt(1000);
        if(chance < 15) {
            // win
            final long reward = HEIST_BASE_COST * 30;
            ctx.getPlayer().incrementBalance(reward);
            database().savePlayer(ctx.getPlayer()).thenAccept(__ ->
                    ctx.sendMessage($(ctx.getLanguage(), "plugins.economy.commands.heist.success")
                            .replace("$amount", "" + reward)
                            .replace("$symbol", helper.getCurrencySymbol(ctx))).thenAccept(___ -> tryDropBox(ctx)));
        } else {
            // lose
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.heist.failure")
                            .replace("$amount", "" + HEIST_BASE_COST)
                            .replace("$symbol", helper.getCurrencySymbol(ctx)));
        }
    }
    
    @Payment(min = 5, max = 1000, fromFirstArg = true)
    @Ratelimit(time = 10)
    @Command(names = "slots", desc = "commands.economy.slots", usage = "slots [amount]",
            examples = {"slots", "slots 100"})
    public void slots(final CommandContext ctx) {
        final User user = ctx.getUser();
        if(!slotsCache.containsKey(user.id())) {
            slotsCache.put(user.id(), new SlotMachine());
        }
        
        final long payment = ctx.getCost();
        final ReelSymbol[][] roll = slotsCache.get(user.id()).roll();
        
        // 10% chance of guaranteed win
        if(random().nextInt(100) < 25) {
            // Middle row == "winning" row
            @SuppressWarnings("UnnecessarilyQualifiedStaticallyImportedElement")
            final ReelSymbol[] symbols = ReelSymbol.values();
            final ReelSymbol win = symbols[random().nextInt(symbols.length)];
            for(int i = 0; i < roll[1].length; i++) {
                roll[1][i] = win;
            }
        }
        
        final boolean win = roll[1][0] == roll[1][1] && roll[1][0] == roll[1][2];
        
        final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.economy.commands.slots.result") + ":\n");
        int counter = 0;
        for(final ReelSymbol[] row : roll) {
            for(final ReelSymbol col : row) {
                sb.append(col.emote);
            }
            if(counter == 1) {
                sb.append('⬅');
            }
            sb.append('\n');
            ++counter;
        }
        sb.append('\n');
        
        if(win) {
            // calc payment and send messages
            final long payout;
            if(roll[1][0] == BOOM) {
                payout = BOOM.worth - payment;
            } else {
                payout = roll[1][0].worth + payment;
            }
            ctx.getPlayer().incrementBalance(payout);
            database().savePlayer(ctx.getPlayer()).thenAccept(__ -> {
                if(roll[1][0] != BOOM) {
                    tryDropBox(ctx);
                }
            });
            //noinspection UnnecessarilyQualifiedStaticallyImportedElement
            if(roll[1][0] == ReelSymbol.BOOM) {
                sb.append($(ctx.getLanguage(), "plugins.economy.commands.slots.boom")
                        .replace("$amount", -payout + "")
                        .replace("$symbol", helper.getCurrencySymbol(ctx)));
            } else {
                sb.append($(ctx.getLanguage(), "plugins.economy.commands.slots.win")
                        .replace("$amount", "" + payout)
                        .replace("$symbol", helper.getCurrencySymbol(ctx)));
            }
        } else {
            sb.append($(ctx.getLanguage(), "plugins.economy.commands.slots.nothing"));
        }
        ctx.sendMessage(sb.toString());
    }
    
    @Ratelimit(time = 20)
    @Command(names = "baltop", desc = "commands.economy.baltop", usage = "baltop", examples = "baltop")
    public void baltop(final CommandContext ctx) {
        database().getStore().sql("SELECT * FROM players ORDER BY (data->>'balance')::integer DESC LIMIT 10;", p -> {
            final ResultSet res = p.executeQuery();
            final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.economy.commands.baltop") + "\n\n");
            final Map<String, Player> players = new LinkedHashMap<>();
            final Map<String, CompletableFuture<User>> futures = new LinkedHashMap<>();
            while(res.next()) {
                final String id = res.getString("id");
                final Player player = new JsonObject(res.getString("data")).mapTo(Player.class);
                players.put(id, player);
                futures.put(id, DiscordCache.user(id).toCompletableFuture());
            }
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).orTimeout(5L, TimeUnit.SECONDS)
                    .thenAccept(__ -> {
                        futures.forEach((id, e) -> {
                            final User user = e.getNow(null);
                            if(user != null) {
                                final Player player = players.get(user.id());
                                sb.append("- ").append(user.username()).append('#').append(user.discriminator()).append(" - ")
                                        .append(player.getBalance()).append(helper.getCurrencySymbol(ctx)).append('\n');
                            } else {
                                final Player player = players.get(id);
                                sb.append("- Unknown User#0000 - ").append(player.getBalance())
                                        .append(helper.getCurrencySymbol(ctx)).append('\n');
                            }
                        });
                        ctx.sendMessage(sb.toString());
                    }).exceptionally(e -> {
                if(e instanceof TimeoutException) {
                    ctx.sendMessage("Couldn't load users in time :(");
                } else {
                    Sentry.capture(e);
                    ctx.sendMessage("\uD83D\uDD25 Couldn't load baltop. Try again later?");
                }
                return null;
            });
        });
    }
    
    @Payment(min = 20, max = 1000, fromFirstArg = true)
    @Ratelimit(time = 60)
    @Command(names = "gamble", desc = "commands.economy.gamble", usage = "gamble [amount]",
            examples = {"gamble", "gamble 100"})
    public void gamble(final CommandContext ctx) {
        final int playerWumpus = random().nextInt(GAMBLE_WUMPUS_COUNT) + 1;
        final int winningWumpus = random().nextInt(GAMBLE_WUMPUS_COUNT) + 1;
        
        final StringBuilder sb = new StringBuilder($(ctx.getLanguage(), "plugins.economy.commands.gamble.result")
                .replace("$amount", "" + ctx.getCost())
                .replace("$symbol", helper.getCurrencySymbol(ctx))
                .replace("$number", playerWumpus + ""))
                .append('\n')
                .append($(ctx.getLanguage(), "plugins.economy.commands.gamble.winner")
                        .replace("$number", "" + winningWumpus));
        
        sb.append("\n\n");
        
        if(playerWumpus == winningWumpus) {
            // Winners get 3x payout
            final long payout = ctx.getCost() * 4;
            ctx.getPlayer().incrementBalance(payout);
            database().savePlayer(ctx.getPlayer()).thenAccept(__ -> tryDropBox(ctx));
            sb.append($(ctx.getLanguage(), "plugins.economy.commands.gamble.success")
                    .replace("$amount", "" + payout)
                    .replace("$symbol", helper.getCurrencySymbol(ctx)));
        } else {
            sb.append($(ctx.getLanguage(), "plugins.economy.commands.gamble.failure")
                    .replace("$amount", "" + ctx.getCost())
                    .replace("$symbol", helper.getCurrencySymbol(ctx)));
        }
        ctx.sendMessage(sb.toString());
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Command(names = {"items", "inventory"}, desc = "commands.economy.items", usage = "items", examples = "items")
    public void items(final CommandContext ctx) {
        final Player user = ctx.getPlayer();
        final EmbedBuilder b = new EmbedBuilder();
        if(user.getItems() != null && !user.getItems().isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            
            final Map<Item, Long> inv = user.getItems();
            
            Lists.partition(new ArrayList<>(inv.keySet()), 2)
                    .forEach(e -> e.forEach(i -> sb.append(i.getEmote()).append(' ').append(i.getName())
                            .append(" `x").append(inv.get(i)).append("`, ")));
            final String str = sb.toString();
            final String sb2 = str.substring(0, str.length() - 2) + "\n\nWeight: `" +
                    user.calculateInventoryWeight() + "`/`" + MAX_INV_WEIGHT + "`\n";
            b.field("Items", sb2.trim(), false);
        } else {
            b.title("Items").description($(ctx.getLanguage(), "plugins.economy.commands.inventory.empty"));
        }
        ctx.sendMessage(b.build());
    }
    
    @Command(names = {"boxes", "box", "boxen"}, desc = "commands.economy.box", usage = {"boxes", "boxes open <type>"},
            examples = {"boxes", "boxes open toolbox"})
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void boxes(final CommandContext ctx) {
        final Player user = ctx.getPlayer();
        if(ctx.getArgs().isEmpty()) {
            final EmbedBuilder b = new EmbedBuilder();
            
            if(user.getBoxes() != null && !user.getBoxes().isEmpty()) {
                final StringBuilder sb = new StringBuilder();
                
                final Map<Box, Long> inv = user.getBoxes();
                
                Lists.partition(new ArrayList<>(inv.keySet()), 2)
                        .forEach(e -> e.forEach(i -> sb.append(i.getName()).append(" `x").append(inv.get(i)).append("`, ")));
                final String str = sb.toString();
                b.title("Your Boxes").description(str.substring(0, str.length() - 2).trim())
                        .field("Hint", $(ctx.getLanguage(), "plugins.economy.commands.boxes.open"), false);
            } else {
                b.title("Boxes").description($(ctx.getLanguage(), "plugins.economy.commands.boxes.none"));
            }
            ctx.sendMessage(b.build());
        } else {
            switch(ctx.getArgs().get(0).toLowerCase()) {
                case "open": {
                    if(ctx.getArgs().size() > 1) {
                        final String boxName = ctx.getArgs().get(1).toLowerCase();
                        Box type = null;
                        for(final Box value : Box.values()) {
                            if(value.getName().equalsIgnoreCase(boxName)) {
                                type = value;
                                break;
                            }
                        }
                        if(type != null) {
                            if(ctx.getPlayer().getBoxes().containsKey(type)) {
                                ctx.getPlayer().removeOneFromBoxes(type);
                                final List<Item> loot = LootTables.generateLoot(type.getLootTable(), 1, 5, false);
                                ctx.getPlayer().addToInventory(loot);
                                database().savePlayer(ctx.getPlayer());
                                
                                final List<String> items = new ArrayList<>();
                                loot.forEach(e -> items.add(e.getName() + ' ' + e.getEmote()));
                                
                                ctx.sendMessage(
                                        $(ctx.getLanguage(), "plugins.economy.commands.boxes.opened-box")
                                                .replace("$box", type.getName())
                                                .replace("$items", String.join(", ", items))
                                );
                            } else {
                                ctx.sendMessage(
                                        $(ctx.getLanguage(), "plugins.economy.commands.boxes.dont-have-box"));
                            }
                        } else {
                            ctx.sendMessage(
                                    $(ctx.getLanguage(), "plugins.economy.commands.boxes.need-box-type"));
                        }
                    } else {
                        ctx.sendMessage(
                                $(ctx.getLanguage(), "plugins.economy.commands.boxes.need-box-type"));
                    }
                    break;
                }
                case "default": {
                    ctx.sendMessage(
                            $(ctx.getLanguage(), "plugins.economy.commands.boxes.unknown"));
                    break;
                }
            }
        }
    }
    
    @Command(names = "market", desc = "commands.economy.market", usage = "market", examples = "market")
    public void market(final CommandContext ctx) {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < Item.values().length; i++) {
            final Item item = Item.values()[i];
            sb.append(item.getEmote()).append(' ').append(item.getName());
            if(item.isCanBuy()) {
                sb.append(" B: `").append(item.getBuyValue())
                        .append("` S: `").append(item.getSellValue()).append('`');
            } else {
                sb.append(" S: `").append(item.getSellValue()).append('`');
            }
            
            if(i % 2 == 1 && i != Item.values().length - 1) {
                sb.append(",\n");
            } else //noinspection StatementWithEmptyBody
                if(i == Item.values().length - 1) {
                    // Intentional no-op
                } else {
                    sb.append(", ");
                }
        }
        final EmbedBuilder b = new EmbedBuilder()
                .title("Mewna Market")
                .field("Items", sb.toString().trim(), false)
                .field("Help", $(ctx.getLanguage(), "plugins.economy.commands.market.buy-sell"),
                        false);
        ctx.sendMessage(b.build());
    }
    
    @Command(names = "buy", desc = "commands.economy.buy", usage = "buy <item name> [amount]",
            examples = {"buy pickaxe", "buy burger 10"})
    public void buy(final CommandContext ctx) {
        final String channelId = ctx.getMessage().channelId();
        if(isWeighedDown(ctx.getPlayer())) {
            catnip().rest().channel().sendMessage(channelId,
                    $(ctx.getLanguage(), "plugins.economy.commands.buy.full-inventory"))
            ;
            return;
        }
        final List<String> args = ctx.getArgs();
        final Player player = ctx.getPlayer();
        if(!args.isEmpty()) {
            final String itemName = args.remove(0);
            final Optional<Item> maybeItem = Arrays.stream(Item.values())
                    .filter(e -> e.getName().equalsIgnoreCase(itemName))
                    .findFirst();
            if(maybeItem.isPresent()) {
                final Item item = maybeItem.get();
                if(item.isCanBuy()) {
                    final long amount;
                    if(args.isEmpty()) {
                        amount = 1;
                    } else {
                        try {
                            amount = Long.parseLong(args.get(0));
                            if(amount <= 0) {
                                throw new IllegalArgumentException();
                            }
                        } catch(final Exception e) {
                            catnip().rest().channel().sendMessage(channelId,
                                    $(ctx.getLanguage(), "plugins.economy.commands.buy.invalid-number")
                                            .replace("$number", args.get(0)));
                            return;
                        }
                    }
                    
                    final long cost = amount * item.getBuyValue();
                    
                    final ImmutablePair<Boolean, Long> res = helper.handlePayment(ctx, "" + cost, cost, cost);
                    if(res.left) {
                        // Money taken, add item(s)
                        player.addAllToInventory(ImmutableMap.of(item, amount));
                        database().savePlayer(player);
                        catnip().rest().channel().sendMessage(channelId,
                                $(ctx.getLanguage(), "plugins.economy.commands.buy.buy-success")
                                        .replace("$number", "" + amount)
                                        .replace("$thing", item.getName())
                                        .replace("$amount", "" + cost)
                                        .replace("$symbol", helper.getCurrencySymbol(ctx)));
                    }
                } else {
                    catnip().rest().channel().sendMessage(channelId, $(ctx.getLanguage(), "plugins.economy.commands.buy.cant-buy"));
                }
            } else {
                catnip().rest().channel().sendMessage(channelId, $(ctx.getLanguage(), "plugins.economy.commands.buy.item-doesnt-exist"));
            }
        } else {
            catnip().rest().channel().sendMessage(channelId, $(ctx.getLanguage(), "plugins.economy.commands.buy.no-item"));
        }
    }
    
    @Command(names = "sell", desc = "commands.economy.sell", usage = "sell <item name> [amount]",
            examples = {"sell pickaxe", "sell burger 10"})
    public void sell(final CommandContext ctx) {
        final List<String> args = ctx.getArgs();
        if(!args.isEmpty()) {
            final String itemName = args.remove(0);
            final Optional<Item> maybeItem = Arrays.stream(Item.values())
                    .filter(e -> e.getName().equalsIgnoreCase(itemName))
                    .findFirst();
            if(maybeItem.isPresent()) {
                final Item item = maybeItem.get();
                final long amount;
                if(args.isEmpty()) {
                    amount = 1;
                } else {
                    try {
                        amount = Long.parseLong(args.get(0));
                        if(amount <= 0) {
                            throw new IllegalArgumentException();
                        }
                    } catch(final Exception e) {
                        ctx.sendMessage(
                                $(ctx.getLanguage(), "plugins.economy.commands.sell.invalid-number")
                                        .replace("$number", args.get(0)));
                        return;
                    }
                }
                
                final long payment = amount * item.getSellValue();
                
                if(ctx.getPlayer().hasItem(item)) {
                    if(ctx.getPlayer().getItems().get(item) >= amount) {
                        ctx.getPlayer().removeAllFromInventory(ImmutableMap.of(item, amount));
                        ctx.getPlayer().incrementBalance(payment);
                        database().savePlayer(ctx.getPlayer());
                        
                        ctx.sendMessage(
                                $(ctx.getLanguage(), "plugins.economy.commands.sell.sell-success")
                                        .replace("$number", "" + amount)
                                        .replace("$thing", item.getName())
                                        .replace("$amount", "" + payment)
                                        .replace("$symbol", helper.getCurrencySymbol(ctx)));
                    } else {
                        ctx.sendMessage(
                                $(ctx.getLanguage(), "plugins.economy.commands.sell.not-enough-item"));
                    }
                } else {
                    ctx.sendMessage(
                            $(ctx.getLanguage(), "plugins.economy.commands.sell.cant-sell"));
                }
            } else {
                ctx.sendMessage(
                        $(ctx.getLanguage(), "plugins.economy.commands.sell.item-doesnt-exist"));
            }
        } else {
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.sell.no-item"));
        }
    }
    
    @Ratelimit(time = 15)
    @Command(names = "mine", desc = "commands.economy.mine", usage = "mine", examples = "mine")
    public void mine(final CommandContext ctx) {
        if(isWeighedDown(ctx.getPlayer())) {
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.mine.full-inventory"));
            return;
        }
        if(!ctx.getPlayer().hasItem(Item.PICKAXE)) {
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.mine.no-pick"));
            return;
        }
        if(LootTables.chance(8)) {
            ctx.getPlayer().removeOneFromInventory(Item.PICKAXE);
            database().savePlayer(ctx.getPlayer());
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.mine.pick-break"));
            return;
        }
        final List<Item> loot = LootTables.generateLoot(LootTables.GEMS, 0, 3, true);
        if(loot.isEmpty()) {
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.mine.got-dust"));
        } else {
            final StringBuilder sb = new StringBuilder();
            final Map<Item, Long> count = lootToMap(loot);
            count.keySet().forEach(e -> sb.append(e.getEmote()).append(" `x").append(count.get(e)).append("`\n"));
            ctx.getPlayer().addAllToInventory(count);
            database().savePlayer(ctx.getPlayer()).thenAccept(__ -> tryDropBox(ctx));
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.mine.success") + '\n' + sb);
        }
    }
    
    @Ratelimit(time = 5)
    @Command(names = "fish", desc = "commands.economy.fish", usage = "fish", examples = "fish")
    public void fish(final CommandContext ctx) {
        if(isWeighedDown(ctx.getPlayer())) {
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.fish.full-inventory"))
            ;
            return;
        }
        if(!ctx.getPlayer().hasItem(Item.FISHING_ROD)) {
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.fish.no-rod"))
            ;
            return;
        }
        if(LootTables.chance(10)) {
            ctx.getPlayer().removeOneFromInventory(Item.FISHING_ROD);
            database().savePlayer(ctx.getPlayer());
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.fish.rod-break"));
            return;
        }
        final List<Item> loot = LootTables.generateLoot(LootTables.FISHING, 1, 5, true);
        if(loot.isEmpty()) {
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.fish.got-water"));
        } else {
            final StringBuilder sb = new StringBuilder();
            final Map<Item, Long> count = lootToMap(loot);
            count.keySet().forEach(e -> sb.append(e.getEmote()).append(" `x").append(count.get(e)).append("`\n"));
            ctx.getPlayer().addAllToInventory(count);
            database().savePlayer(ctx.getPlayer()).thenAccept(__ -> tryDropBox(ctx));
            ctx.sendMessage(
                    $(ctx.getLanguage(), "plugins.economy.commands.fish.success") + '\n' + sb);
        }
    }
    
    private CompletableFuture<Void> tryDropBox(final CommandContext ctx) {
        if(LootTables.chance(random().nextInt(12))) {
            final Future<Void> future = Future.future();
            final Box box = Box.values()[random().nextInt(Box.values().length)];
            ctx.getPlayer().addOneToBoxes(box);
            database().savePlayer(ctx.getPlayer())
                    .thenAccept(__ -> ctx.sendMessage(
                            $(ctx.getLanguage(), "plugins.economy.found-box")
                                    .replace("$box", box.getName()))
                            .thenAccept(___ -> future.complete(null))
                            .exceptionally(e -> {
                                future.complete(null);
                                return null;
                            }))
                    .exceptionally(e -> {
                        Sentry.capture(e);
                        future.complete(null);
                        return null;
                    });
            return SafeVertxCompletableFuture.from(catnip(), future);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
    
    private Map<Item, Long> lootToMap(final Collection<Item> loot) {
        return loot.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
    }
    
    private boolean isWeighedDown(final Player player) {
        return player.calculateInventoryWeight() >= MAX_INV_WEIGHT;
    }
    
    @ToString
    enum ReelSymbol {
        SEVEN(":seven:", 500L),
        GEM(":gem:", 250L),
        DOLLAR(":dollar:", 100L),
        APPLE(":apple:", 50L),
        CHERRY(":cherries:", 10L),
        ZERO(":zero:", 0L),
        BOOM(":boom:", -100L);
        
        private final String emote;
        private final long worth;
        
        ReelSymbol(final String emote, final long worth) {
            this.emote = emote;
            this.worth = worth;
        }
    }
    
    final class Reel {
        private final List<ReelSymbol> symbols = new ArrayList<>(Arrays.asList(
                // @formatter:off
                SEVEN,
                GEM,    GEM,
                DOLLAR, DOLLAR, DOLLAR,
                APPLE,  APPLE,  APPLE,  APPLE,  APPLE,
                CHERRY, CHERRY, CHERRY, CHERRY, CHERRY,
                ZERO,   ZERO,   ZERO,   ZERO,   ZERO,   ZERO,
                BOOM,   BOOM,   BOOM
                // @formatter:on
        ));
        
        Reel() {
            Collections.shuffle(symbols);
        }
    }
    
    final class SlotMachine {
        private final Reel[] reels = {
                new Reel(),
                new Reel(),
                new Reel(),
        };
        
        private ReelSymbol[] pickSymbols(final int r) {
            final Reel reel = reels[r];
            final List<ReelSymbol> symbols = reel.symbols;
            final int last = symbols.size() - 1;
            final int p = random().nextInt(symbols.size());
            final List<ReelSymbol> pick = new ArrayList<>();
            
            // Pick the symbol at index
            pick.add(symbols.get(p));
            
            // Pick the symbol before it
            if(p > 0) {
                pick.add(symbols.get(p - 1));
            } else {
                pick.add(symbols.get(last));
            }
            
            // Pick the symbol after it
            if(p < last) {
                pick.add(symbols.get(p + 1));
            } else {
                pick.add(symbols.get(0));
            }
            return pick.toArray(new ReelSymbol[0]);
        }
        
        ReelSymbol[][] roll() {
            final ReelSymbol[][] res = new ReelSymbol[3][];
            res[0] = pickSymbols(0);
            res[1] = pickSymbols(1);
            res[2] = pickSymbols(2);
            return res;
        }
    }
}
