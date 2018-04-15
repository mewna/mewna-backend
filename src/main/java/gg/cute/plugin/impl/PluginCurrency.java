package gg.cute.plugin.impl;

import gg.cute.cache.entity.User;
import gg.cute.data.Player;
import gg.cute.plugin.BasePlugin;
import gg.cute.plugin.Command;
import gg.cute.plugin.CommandContext;
import gg.cute.plugin.Plugin;
import gg.cute.plugin.ratelimit.Ratelimit;
import gg.cute.util.Time;
import lombok.ToString;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static gg.cute.plugin.impl.PluginCurrency.ReelSymbol.*;
import static gg.cute.plugin.ratelimit.RatelimitType.GUILD;

/**
 * @author amy
 * @since 4/14/18.
 */
@Plugin("currency")
@SuppressWarnings("unused")
public class PluginCurrency extends BasePlugin {
    private static final long SLOTS_BASE = 10;
    private static final long GAMBLE_BASE = 25;
    private static final long HEIST_BASE = 300;
    private static final long CRIME_BASE = 10;
    private static final long DAILY_BASE = 100;
    
    private static final long SLOTS_COOLDOWN = TimeUnit.SECONDS.toMillis(10);
    private static final long GAMBLE_COOLDOWN = TimeUnit.MINUTES.toMillis(1);
    private static final long HEIST_COOLDOWN = TimeUnit.MINUTES.toMillis(5);
    private static final long CRIME_COOLDOWN = TimeUnit.SECONDS.toMillis(20);
    
    @Command(names = {"balance", "bal"}, desc = "Check your balance, or someone else's balance.", usage = "balance [player]",
            examples = {"balance", "balance @someone"})
    public void balance(final CommandContext ctx) {
        if(ctx.getMentions().isEmpty()) {
            final Player player = ctx.getPlayer();
            final long balance = player.getBalance(ctx);
            getRestJDA().sendMessage(ctx.getChannel(),
                    String.format("**You** have **%s%s**.", balance, getCurrencySymbol(ctx))).queue();
        } else {
            final User m = ctx.getMentions().get(0);
            final Player player = getDatabase().getPlayer(m);
            final long balance = player.getBalance(ctx);
            getRestJDA().sendMessage(ctx.getChannel(),
                    String.format("**%s** has **%s%s**.", m.getName(), balance, getCurrencySymbol(ctx))).queue();
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
        final ImmutablePair<Boolean, Long> res = handlePayment(ctx, ctx.getArgs().get(1), 1, Long.MAX_VALUE);
        if(res.left) {
            target.incrementBalance(ctx.getGuild(), res.right);
            getDatabase().savePlayer(target);
            getRestJDA().sendMessage(ctx.getChannel(), String.format("**%s**, you sent **%s%s** to **%s**.",
                    ctx.getUser().getName(), res.right, getCurrencySymbol(ctx), ctx.getMentions().get(0).getName())).queue();
        }
    }
    
    @Command(names = "daily", desc = "Collect some money once a day.", usage = "daily", examples = "daily")
    public void daily(final CommandContext ctx) {
        // TODO: Ratelimit this
        // TODO: Streaks
        ctx.getPlayer().incrementBalance(ctx.getGuild(), DAILY_BASE);
        getDatabase().savePlayer(ctx.getPlayer());
        getRestJDA().sendMessage(ctx.getChannel(), String.format("You collect your daily **%s%s**.", DAILY_BASE,
                getCurrencySymbol(ctx))).queue();
    }
    
    @Ratelimit(time = CRIME_BASE)
    @Command(names = "crime", desc = "Shake down some people for some money.", usage = "crime", examples = "crime")
    public void crime(final CommandContext ctx) {
        getRestJDA().sendMessage(ctx.getChannel().getId(), "<<unimplemented>>").queue();
    }
    
    @Command(names = "heist", desc = "Execute a daring raid on the impenetrable Fort Knick-Knacks.", usage = "heist",
            examples = "heist")
    public void heist(final CommandContext ctx) {
        getRestJDA().sendMessage(ctx.getChannel().getId(), "<<unimplemented>>").queue();
    }
    
    @Command(names = "slots", desc = "Gamble your life away at the slot machines.", usage = "slots [amount]",
            examples = {"slots", "slots 100"})
    public void slots(final CommandContext ctx) {
        getRestJDA().sendMessage(ctx.getChannel().getId(), "<<unimplemented>>").queue();
    }
    
    @Command(names = "gamble", desc = "Bet big on the Wumpus Races.", usage = "gamble [amount]",
            examples = {"gamble", "gamble 100"})
    public void gamble(final CommandContext ctx) {
        getRestJDA().sendMessage(ctx.getChannel().getId(), "<<unimplemented>>").queue();
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
