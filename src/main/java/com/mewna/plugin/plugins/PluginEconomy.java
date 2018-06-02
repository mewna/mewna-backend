package com.mewna.plugin.plugins;

import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.data.Player;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.metadata.Payment;
import com.mewna.plugin.metadata.Ratelimit;
import com.mewna.plugin.plugins.settings.EconomySettings;
import com.mewna.plugin.util.CurrencyHelper;
import com.mewna.util.Time;
import lombok.ToString;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.inject.Inject;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
    
    private static final int GAMBLE_WUMPUS_COUNT = 5;
    
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
        // TODO: Streaks
        final ZoneId zone = ZoneId.systemDefault();
        final LocalDateTime last = Instant.ofEpochMilli(player.getLastDaily()).atZone(zone).toLocalDateTime();
        final LocalDateTime now = LocalDateTime.now();
        if(last.toLocalDate().plusDays(1).toEpochDay() > now.toLocalDate().toEpochDay()) {
            final long nextMillis = TimeUnit.SECONDS.toMillis(last.toLocalDate().plusDays(1).atStartOfDay(zone).toEpochSecond());
            final long nowMillis = TimeUnit.SECONDS.toMillis(now.toEpochSecond(zone.getRules().getOffset(now)));
            getRestJDA().sendMessage(ctx.getChannel(),
                    String.format("You can collect your daily again in **%s**.",
                            Time.toHumanReadableDuration(nextMillis - nowMillis))).queue();
            return;
        }
        
        player.incrementBalance(DAILY_BASE_REWARD);
        player.updateLastDaily();
        getDatabase().savePlayer(player);
        getRestJDA().sendMessage(ctx.getChannel(), String.format("You collect your daily **%s%s**.", DAILY_BASE_REWARD,
                helper.getCurrencySymbol(ctx))).queue();
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
        if(getRandom().nextInt(100) < 10) {
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
        for(final ReelSymbol[] row : roll) {
            for(final ReelSymbol col : row) {
                sb.append(col.emote);
            }
            sb.append('\n');
        }
        
        if(win) {
            // calc payment and send messages
            final long payout = roll[1][0].worth + payment;
            // TODO: Rare bonus chance like I always do
            
            ctx.getPlayer().incrementBalance(payout);
            getDatabase().savePlayer(ctx.getPlayer());
            
            //noinspection UnnecessarilyQualifiedStaticallyImportedElement
            if(roll[1][0] == ReelSymbol.BOOM) {
                sb.append("\nOh no! The slot machine exploded! You pay out **").append(payout).append(helper.getCurrencySymbol(ctx))
                        .append("** to cover the repair cost.");
            } else {
                sb.append("\nYou won **").append(payout).append(helper.getCurrencySymbol(ctx)).append("**!");
            }
            
            getRestJDA().sendMessage(ctx.getChannel(), sb.toString()).queue();
        } else {
            getRestJDA().sendMessage(ctx.getChannel(), sb.append("\nThe slot machine paid out nothing...").toString()).queue();
        }
    }
    
    @Payment(min = 20, max = 1000, fromFirstArg = true)
    @Ratelimit(time = 60)
    @Command(names = "gamble", desc = "Bet big on the Wumpus Races.", usage = "gamble [amount]",
            examples = {"gamble", "gamble 100"})
    public void gamble(final CommandContext ctx) {
        // TODO: Allow a way for a player to choose this
        final int playerWumpus = getRandom().nextInt(GAMBLE_WUMPUS_COUNT);
        final int winningWumpus = getRandom().nextInt(GAMBLE_WUMPUS_COUNT);
        
        final StringBuilder sb = new StringBuilder("You head off to the Wumpus Races to gamble your life away.");
        sb.append("You bet **").append(ctx.getCost()).append(helper.getCurrencySymbol(ctx)).append("** on Wumpus **#")
                .append(playerWumpus).append("**.\n\n");
        sb.append("And the winner is Wumpus **#").append(winningWumpus).append("**!\n\n");
        
        if(playerWumpus == winningWumpus) {
            final long payout = ctx.getCost() * 5; // TODO: Might be high...?
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
