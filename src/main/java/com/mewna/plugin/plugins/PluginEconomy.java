package com.mewna.plugin.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
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
import lombok.ToString;
import net.dv8tion.jda.core.EmbedBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.inject.Inject;
import java.io.IOException;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.mewna.data.Player.MAX_INV_WEIGHT;
import static com.mewna.plugin.plugins.PluginEconomy.ReelSymbol.*;

/**
 * @author amy
 * @since 5/19/18.
 */
@SuppressWarnings("unused")
@Plugin(name = "Economy", desc = "Let people earn and spend money.", settings = EconomySettings.class)
public class PluginEconomy extends BasePlugin {
    private static final long SLOTS_BASE_COST = 10;
    private static final long GAMBLE_BASE_COST = 25;
    private static final long HEIST_BASE_COST = 300;
    private static final long CRIME_BASE_COST = 10;
    
    private static final long DAILY_BASE_REWARD = 100;
    
    private static final int GAMBLE_WUMPUS_COUNT = 4;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Map<String, SlotMachine> slotsCache = new HashMap<>();
    @Inject
    private CurrencyHelper helper;
    
    @Command(names = {"balance", "bal"}, desc = "Check your balance, or someone else's balance.", usage = "balance [player]",
            examples = {"balance", "balance @someone"})
    public void balance(final CommandContext ctx) {
        if(ctx.getMentions().isEmpty()) {
            final Player player = ctx.getPlayer();
            final long balance = player.getBalance();
            getRestJDA().sendMessage(ctx.getChannel(),
                    String.format("**You** have **%s%s**.", balance, helper.getCurrencySymbol(ctx))).queue();
        } else {
            final User m = ctx.getMentions().get(0);
            final Player player = getDatabase().getPlayer(m);
            final long balance = player.getBalance();
            getRestJDA().sendMessage(ctx.getChannel(),
                    String.format("**%s** has **%s%s**.", m.getName(), balance, helper.getCurrencySymbol(ctx))).queue();
        }
    }
    
    @Command(names = "pay", desc = "Send money to another user.", usage = "pay <mention> <amount>",
            examples = "pay @someone 100")
    public void pay(final CommandContext ctx) {
        if(ctx.getMentions().isEmpty()) {
            getRestJDA().sendMessage(ctx.getChannel(), "You need to mention someone to pay.").queue();
            return;
        }
        if(ctx.getArgs().size() < 2) {
            getRestJDA().sendMessage(ctx.getChannel(), "You need to mention someone to pay, and specify an amount to pay.").queue();
            return;
        }
        final Player sender = ctx.getPlayer();
        final Player target = getDatabase().getPlayer(ctx.getMentions().get(0));
        if(target.getId().equalsIgnoreCase(sender.getId())) {
            getRestJDA().sendMessage(ctx.getChannel(), "Nice try ;)").queue();
            return;
        }
        final ImmutablePair<Boolean, Long> res = helper.handlePayment(ctx, ctx.getArgs().get(1), 1, Long.MAX_VALUE);
        if(res.left) {
            target.incrementBalance(res.right);
            getDatabase().savePlayer(target);
            getRestJDA().sendMessage(ctx.getChannel(), String.format("**%s**, you sent **%s%s** to **%s**.",
                    ctx.getUser().getName(), res.right, helper.getCurrencySymbol(ctx), ctx.getMentions().get(0).getName())).queue();
        }
    }
    
    @Command(names = "daily", desc = "Collect some money once a day.", usage = "daily", examples = "daily")
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
            getRestJDA().sendMessage(ctx.getChannel(),
                    String.format("You can collect your daily again in **%s**.",
                            Time.toHumanReadableDuration(nextMillis - nowMillis))).queue();
            return;
        }
        final boolean streak;
        // check streak
        streak = last.toLocalDate().plusDays(2).toEpochDay() >= now.toLocalDate().toEpochDay();
        if(streak) {
            player.incrementDailyStreak();
            final long bonus = 100 + 10 * (player.getDailyStreak() - 1);
            player.incrementBalance(DAILY_BASE_REWARD + bonus);
            getRestJDA().sendMessage(ctx.getChannel(), String.format("You collect your daily **%s%s**.\n\nStreak up! New streak: `%sx`, bonus: %s%s.",
                    DAILY_BASE_REWARD, helper.getCurrencySymbol(ctx), player.getDailyStreak(), bonus, helper.getCurrencySymbol(ctx))).queue();
        } else {
            player.resetDailyStreak();
            player.incrementBalance(DAILY_BASE_REWARD);
            getRestJDA().sendMessage(ctx.getChannel(), String.format("You collect your daily **%s%s**." +
                            "\n\nIt's been more than 2 days since you last collected your %sdaily, so your streak has been reset.",
                    DAILY_BASE_REWARD, helper.getCurrencySymbol(ctx), ctx.getPrefix())).queue();
        }
        
        player.updateLastDaily();
        getDatabase().savePlayer(player);
    }
    
    @Ratelimit(time = 20)
    @Command(names = "crime", desc = "Shake down some people for some money.", usage = "crime", examples = "crime")
    public void crime(final CommandContext ctx) {
        final int choice = getRandom().nextInt(10) + 1;
        final int amount = getRandom().nextInt(Math.toIntExact(CRIME_BASE_COST)) + 1;
        final String text;
        switch(choice) {
            case 1: {
                text = String.format("You steal candy from a baby and sell it back to him, netting %s%s.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            case 2: {
                text = String.format("You sell someone a shiny (but worthless) rock for %s%s.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            case 3: {
                text = String.format("You pretend to get hit by someone's Wumpus, and scam Bamboozle Insurance:tm: out of %s%s.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            case 4: {
                text = String.format("You successfully steal a cash register, and take the %s%s inside.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            case 5: {
                text = String.format("You sell someone a rotten :potato: for %s%s.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            case 6: {
                text = String.format("You robbed the Society of Schmoogaloo and ended up in a lake, but still managed to steal %s%s.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            case 7: {
                text = String.format("You start the Cult of Wumpus, and scam the cult members out of %s%s.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            case 8: {
                text = String.format("You kidnap someone's pet Wumpus, only returning it after they pay the ransom of %s%s.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            case 9: {
                text = String.format("You sell candy to young school children, bringing in %s%s.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            case 10: {
                text = String.format("You sell a Wumpus to the circus for %s%s.", amount, helper.getCurrencySymbol(ctx));
                break;
            }
            default: {
                text = ":fire:";
                break;
            }
        }
        ctx.getPlayer().incrementBalance(amount);
        getDatabase().savePlayer(ctx.getPlayer());
        getRestJDA().sendMessage(ctx.getChannel().getId(), text).queue();
    }
    
    @Payment(min = HEIST_BASE_COST)
    @Ratelimit(time = 5 * 60)
    @Command(names = "heist", desc = "Execute a daring raid on the impenetrable Fort Knick-Knacks.", usage = "heist",
            examples = "heist")
    public void heist(final CommandContext ctx) {
        final int chance = getRandom().nextInt(1000);
        if(chance < 125) {
            // win
            final long reward = HEIST_BASE_COST * 10;
            ctx.getPlayer().incrementBalance(reward);
            getDatabase().savePlayer(ctx.getPlayer());
            
            getRestJDA().sendMessage(ctx.getChannel(), String.format("You break into Fort Knick-Knacks and steal %s%s from inside.",
                    reward, helper.getCurrencySymbol(ctx))).queue();
        } else {
            // lose
            getRestJDA().sendMessage(ctx.getChannel(), String.format("Oh no! The guards caught you! They take %s%s from you and let you go.",
                    HEIST_BASE_COST, helper.getCurrencySymbol(ctx))).queue();
        }
    }
    
    @Payment(min = 5, max = 1000, fromFirstArg = true)
    @Ratelimit(time = 10)
    @Command(names = "slots", desc = "Gamble your life away at the slot machines.", usage = "slots [amount]",
            examples = {"slots", "slots 100"})
    public void slots(final CommandContext ctx) {
        final User user = ctx.getUser();
        if(!slotsCache.containsKey(user.getId())) {
            slotsCache.put(user.getId(), new SlotMachine());
        }
        
        final long payment = ctx.getCost();
        final ReelSymbol[][] roll = slotsCache.get(user.getId()).roll();
        
        // 10% chance of guaranteed win
        if(getRandom().nextInt(100) < 25) {
            // Middle row == "winning" row
            @SuppressWarnings("UnnecessarilyQualifiedStaticallyImportedElement")
            final ReelSymbol[] symbols = ReelSymbol.values();
            final ReelSymbol win = symbols[getRandom().nextInt(symbols.length)];
            for(int i = 0; i < roll[1].length; i++) {
                roll[1][i] = win;
            }
        }
        
        final boolean win = roll[1][0] == roll[1][1] && roll[1][0] == roll[1][2];
        
        final StringBuilder sb = new StringBuilder("The slot machines rolled up:\n");
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
        
        if(win) {
            // calc payment and send messages
            final long payout = roll[1][0].worth + payment;
            // TODO: Rare bonus chance like I always do
            
            ctx.getPlayer().incrementBalance(payout);
            getDatabase().savePlayer(ctx.getPlayer());
            
            //noinspection UnnecessarilyQualifiedStaticallyImportedElement
            if(roll[1][0] == ReelSymbol.BOOM) {
                sb.append("\nOh no! The slot machine exploded! You pay out **").append(-payout).append(helper.getCurrencySymbol(ctx))
                        .append("** to cover the repair cost.");
            } else {
                sb.append("\nYou won **").append(payout).append(helper.getCurrencySymbol(ctx)).append("**!");
            }
            
            getRestJDA().sendMessage(ctx.getChannel(), sb.toString()).queue();
        } else {
            getRestJDA().sendMessage(ctx.getChannel(), sb.append("\nThe slot machine paid out nothing...").toString()).queue();
        }
    }
    
    @Ratelimit(time = 20)
    @Command(names = "baltop", desc = "See the 10 richest users.", usage = "baltop", examples = "baltop")
    public void baltop(final CommandContext ctx) {
        getDatabase().getStore().sql("SELECT * FROM players ORDER BY (data->>'balance')::integer DESC LIMIT 10;", p -> {
            final ResultSet res = p.executeQuery();
            final StringBuilder sb = new StringBuilder("The 10 richest users are:\n\n");
            while(res.next()) {
                try {
                    final String id = res.getString("id");
                    final Player player = MAPPER.readValue(res.getString("data"), Player.class);
                    final User user = getCache().getUser(id);
                    sb.append("- ").append(user.getName()).append('#').append(user.getDiscriminator()).append(" - ")
                            .append(player.getBalance()).append(helper.getCurrencySymbol(ctx)).append('\n');
                } catch(final IOException e) {
                    Sentry.capture(e);
                    e.printStackTrace();
                }
            }
            getRestJDA().sendMessage(ctx.getChannel(), sb.toString()).queue();
        });
    }
    
    @Payment(min = 20, max = 1000, fromFirstArg = true)
    @Ratelimit(time = 60)
    @Command(names = "gamble", desc = "Bet big on the Wumpus Races.", usage = "gamble [amount]",
            examples = {"gamble", "gamble 100"})
    public void gamble(final CommandContext ctx) {
        // TODO: Allow a way for a player to choose this
        final int playerWumpus = getRandom().nextInt(GAMBLE_WUMPUS_COUNT) + 1;
        final int winningWumpus = getRandom().nextInt(GAMBLE_WUMPUS_COUNT) + 1;
        
        final StringBuilder sb = new StringBuilder("You head off to the Wumpus Races to gamble your life away. ");
        sb.append("You bet **").append(ctx.getCost()).append(helper.getCurrencySymbol(ctx)).append("** on Wumpus **#")
                .append(playerWumpus).append("**.\n\n");
        sb.append("And the winner is Wumpus **#").append(winningWumpus).append("**!\n\n");
        
        if(playerWumpus == winningWumpus) {
            // Winners get 3x payout
            final long payout = ctx.getCost() * 3;
            ctx.getPlayer().incrementBalance(payout);
            getDatabase().savePlayer(ctx.getPlayer());
            sb.append("You bet on the right one! You win **").append(payout).append(helper.getCurrencySymbol(ctx))
                    .append("** for betting right!");
        } else {
            sb.append("You bet on a loser! Your **").append(ctx.getCost()).append(helper.getCurrencySymbol(ctx))
                    .append("** is gone forever...");
        }
        getRestJDA().sendMessage(ctx.getChannel(), sb.toString()).queue();
    }
    
    @Command(names = {"items", "inventory"}, desc = "View your items.", usage = "items", examples = "items")
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
            b.addField("Items", sb2.trim(), false);
        } else {
            b.setTitle("Items").setDescription("You have nothing!");
        }
        getRestJDA().sendMessage(ctx.getChannel(), b.build()).queue();
    }
    
    //@Command(names = {"boxes", "boxen"}, desc = "View your boxes.", usage = "boxes", examples = "boxes")
    public void boxes(final CommandContext ctx) {
        final Player user = ctx.getPlayer();
        final EmbedBuilder b = new EmbedBuilder();
        if(user.getBoxes() != null && !user.getBoxes().isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            
            final Map<Box, Long> inv = user.getBoxes();
            
            Lists.partition(new ArrayList<>(inv.keySet()), 2)
                    .forEach(e -> e.forEach(i -> sb.append(i.getName()).append(" `x").append(inv.get(i)).append("`, ")));
            final String str = sb.toString();
            b.addField("Boxes", str.substring(0, str.length() - 2).trim(), false);
        } else {
            b.setTitle("Boxes").setDescription("You have no boxes!");
        }
        getRestJDA().sendMessage(ctx.getChannel(), b.build()).queue();
    }
    
    /*
    @Command(names = "fillbox", desc = "", usage = "", examples = "")
    public void boxme(final CommandContext ctx) {
        final Player user = ctx.getPlayer();
        user.addToBoxes(Arrays.asList(Box.values()));
        getDatabase().savePlayer(user);
        
        getRestJDA().sendMessage(ctx.getChannel(), "thumbsup").queue();
    }
    */
    
    @Command(names = "market", desc = "Show available items", usage = "market", examples = "market")
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
        final EmbedBuilder b = new EmbedBuilder().addField("Market", sb.toString().trim(), false)
                .addField("", String.format("You can buy and sell items with `%sbuy` and `%ssell`.", ctx.getPrefix(), ctx.getPrefix()),
                        false);
        getRestJDA().sendMessage(ctx.getChannel(), b.build()).queue();
    }
    
    @Command(names = "buy", desc = "Buy some items.", usage = "buy <item name> [amount]",
            examples = {"buy pickaxe", "buy burger 10"})
    public void buy(final CommandContext ctx) {
        if(isWeighedDown(ctx.getPlayer())) {
            getRestJDA().sendMessage(ctx.getChannel(),
                    "Your inventory is full! Try selling some stuff first.")
                    .queue();
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
                            getRestJDA().sendMessage(ctx.getChannel(),
                                    String.format("%s isn't a valid number", args.get(0))).queue();
                            return;
                        }
                    }
                    
                    final long cost = amount * item.getBuyValue();
                    
                    final ImmutablePair<Boolean, Long> res = helper.handlePayment(ctx, "" + cost, cost, cost);
                    if(res.left) {
                        // Money taken, add item(s)
                        player.addAllToInventory(ImmutableMap.of(item, amount));
                        getDatabase().savePlayer(player);
                        getRestJDA().sendMessage(ctx.getChannel(), String.format("You bought %s %s for %s%s.",
                                amount, item.getName(), cost, helper.getCurrencySymbol(ctx))).queue();
                    }
                } else {
                    getRestJDA().sendMessage(ctx.getChannel(), "That item cannot be bought.").queue();
                }
            } else {
                getRestJDA().sendMessage(ctx.getChannel(), "That item doesn't exist.").queue();
            }
        } else {
            getRestJDA().sendMessage(ctx.getChannel(), "You have to tell me what to buy.").queue();
        }
    }
    
    @Command(names = "sell", desc = "Sell some items.", usage = "sell <item name> [amount]",
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
                        getRestJDA().sendMessage(ctx.getChannel(),
                                String.format("%s isn't a valid number", args.get(0))).queue();
                        return;
                    }
                }
                
                final long payment = amount * item.getSellValue();
                
                if(ctx.getPlayer().hasItem(item)) {
                    if(ctx.getPlayer().getItems().get(item) >= amount) {
                        ctx.getPlayer().removeAllFromInventory(ImmutableMap.of(item, amount));
                        ctx.getPlayer().incrementBalance(payment);
                        getDatabase().savePlayer(ctx.getPlayer());
                        getRestJDA().sendMessage(ctx.getChannel(), String.format("You sold %s %s for %s%s.",
                                amount, item.getName(), payment, helper.getCurrencySymbol(ctx))).queue();
                    } else {
                        getRestJDA().sendMessage(ctx.getChannel(), "You don't have enough of that item.").queue();
                    }
                } else {
                    getRestJDA().sendMessage(ctx.getChannel(), "You don't have that item.").queue();
                }
            } else {
                getRestJDA().sendMessage(ctx.getChannel(), "That item doesn't exist.").queue();
            }
        } else {
            getRestJDA().sendMessage(ctx.getChannel(), "You have to tell me what to sell.").queue();
        }
    }
    
    @Ratelimit(time = 20)
    @Command(names = "mine", desc = "Go mining for shines!", usage = "mine", examples = "mine")
    public void mine(final CommandContext ctx) {
        if(isWeighedDown(ctx.getPlayer())) {
            getRestJDA().sendMessage(ctx.getChannel(),
                    "Your inventory is full! Try selling some stuff first.")
                    .queue();
            return;
        }
        if(!ctx.getPlayer().hasItem(Item.PICKAXE)) {
            getRestJDA().sendMessage(ctx.getChannel(),
                    String.format("You don't have a `pickaxe`, so you can't mine. Perhaps you should `%sbuy` one...",
                            ctx.getPrefix()))
                    .queue();
            return;
        }
        if(LootTables.chance(5)) {
            ctx.getPlayer().removeOneFromInventory(Item.PICKAXE);
            getDatabase().savePlayer(ctx.getPlayer());
            getRestJDA().sendMessage(ctx.getChannel(), "Oh no! Your pickaxe broke when you tried to use it!").queue();
            return;
        }
        final List<Item> loot = LootTables.generateLoot(LootTables.GEMS, 0, 2);
        if(loot.isEmpty()) {
            getRestJDA().sendMessage(ctx.getChannel(), "You mined all day, but found nothing but dust.").queue();
        } else {
            final StringBuilder sb = new StringBuilder();
            final Map<Item, Long> count = lootToMap(loot);
            count.keySet().forEach(e -> sb.append(e.getEmote()).append(" `x").append(count.get(e)).append("`\n"));
            ctx.getPlayer().addAllToInventory(count);
            getDatabase().savePlayer(ctx.getPlayer());
            getRestJDA().sendMessage(ctx.getChannel(), "You dug up:\n" + sb).queue();
        }
    }
    
    @Ratelimit(time = 10)
    @Command(names = "fish", desc = "Go fishing for tasty fish!", usage = "fish", examples = "fish")
    public void fish(final CommandContext ctx) {
        if(isWeighedDown(ctx.getPlayer())) {
            getRestJDA().sendMessage(ctx.getChannel(),
                    "Your inventory is full! Try selling some stuff first.")
                    .queue();
            return;
        }
        if(!ctx.getPlayer().hasItem(Item.FISHING_ROD)) {
            getRestJDA().sendMessage(ctx.getChannel(),
                    String.format("You don't have a `fishingrod`, so you can't fish. Perhaps you should `%sbuy` one...",
                            ctx.getPrefix()))
                    .queue();
            return;
        }
        if(LootTables.chance(5)) {
            ctx.getPlayer().removeOneFromInventory(Item.FISHING_ROD);
            getDatabase().savePlayer(ctx.getPlayer());
            getRestJDA().sendMessage(ctx.getChannel(), "Oh no! Your fishing rod broke when you tried to use it!").queue();
            return;
        }
        final List<Item> loot = LootTables.generateLoot(LootTables.FISHING, 1, 3);
        if(loot.isEmpty()) {
            getRestJDA().sendMessage(ctx.getChannel(), "You fished all day, but found nothing but empty water.").queue();
        } else {
            final StringBuilder sb = new StringBuilder();
            final Map<Item, Long> count = lootToMap(loot);
            count.keySet().forEach(e -> sb.append(e.getEmote()).append(" `x").append(count.get(e)).append("`\n"));
            ctx.getPlayer().addAllToInventory(count);
            getDatabase().savePlayer(ctx.getPlayer());
            getRestJDA().sendMessage(ctx.getChannel(), "You fished up:\n" + sb).queue();
        }
    }
    
    public boolean tryDropBox(final CommandContext ctx) {
        if(LootTables.chance(50)) {
            return false;
        }
        return false;
    }
    
    /* // TODO
    public boolean tryDropItem(final CommandContext ctx) {
        return false;
    }
    */
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
            final int p = getRandom().nextInt(symbols.size());
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
