package com.mewna.plugin.util;

import com.mewna.Mewna;
import com.mewna.cache.entity.Guild;
import com.mewna.data.GuildSettings;
import com.mewna.data.Player;
import com.mewna.plugin.CommandContext;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.inject.Inject;

import static com.mewna.plugin.util.CurrencyHelper.PaymentResult.*;

/**
 * @author amy
 * @since 4/15/18.
 */
public final class CurrencyHelper {
    private static final String CURRENCY_SYMBOL = ":white_flower:";
    @Inject
    private Mewna mewna;
    
    @SuppressWarnings("WeakerAccess")
    public final ImmutablePair<Boolean, Long> handlePayment(final CommandContext ctx, final String maybeAmount, final long min, final long max) {
        final ImmutablePair<PaymentResult, Long> check = checkPayment(ctx.getGuild(), ctx.getPlayer(), maybeAmount, min, max);
        final String symbol = getCurrencySymbol(ctx);
        switch(check.left) {
            case BAD_EMPTY: {
                mewna.getRestJDA().sendMessage(ctx.getChannel(), "You can't pay nothing!").queue();
                return ImmutablePair.of(false, -1L);
            }
            case BAD_NOT_NUM: {
                mewna.getRestJDA().sendMessage(ctx.getChannel(), String.format("`%s` isn't a number!", maybeAmount)).queue();
                return ImmutablePair.of(false, -1L);
            }
            case BAD_TOO_POOR_NO_BAL: {
                mewna.getRestJDA().sendMessage(ctx.getChannel(), "You don't have any money!").queue();
                return ImmutablePair.of(false, -1L);
            }
            case BAD_TOO_POOR: {
                mewna.getRestJDA().sendMessage(ctx.getChannel(), String.format("You tried to spend %s%s, but you only have %s%s!",
                        maybeAmount, symbol, ctx.getPlayer().getBalance(ctx.getGuild()), symbol)).queue();
                return ImmutablePair.of(false, -1L);
            }
            case BAD_TOO_CHEAP: {
                mewna.getRestJDA().sendMessage(ctx.getChannel(), String.format("You tried to spend %s%s, but you need to spend at least %s%s!",
                        maybeAmount, symbol, min, symbol)).queue();
                return ImmutablePair.of(false, -1L);
            }
            case BAD_TOO_MUCH: {
                mewna.getRestJDA().sendMessage(ctx.getChannel(), String.format("You tried to spend %s%s, but you can only spend up to %s%s!",
                        maybeAmount, symbol, max, symbol)).queue();
                return ImmutablePair.of(false, -1L);
            }
            case OK: {
                ctx.getPlayer().incrementBalance(ctx.getGuild(), -check.right);
                mewna.getDatabase().savePlayer(ctx.getPlayer());
                return ImmutablePair.of(true, check.right);
            }
            default: {
                throw new IllegalStateException("Got invalid payment result state: " + check.left + "!?");
            }
        }
    }
    
    public final ImmutablePair<PaymentResult, Long> checkPayment(final Guild guild, final Player player, final String maybeAmount, final long min, final long max) {
        final long balance = player.getBalance(guild);
        final long payment;
        try {
            payment = Long.parseLong(maybeAmount);
        } catch(final NullPointerException | NumberFormatException e) {
            if(maybeAmount == null || maybeAmount.trim().isEmpty()) {
                return ImmutablePair.of(BAD_EMPTY, -1L);
            } else {
                return ImmutablePair.of(BAD_NOT_NUM, -1L);
            }
        }
        if(balance <= 0) {
            return ImmutablePair.of(BAD_TOO_POOR_NO_BAL, -1L);
        }
        if(balance < min) {
            return ImmutablePair.of(BAD_TOO_POOR, -1L);
        }
        if(payment > balance) {
            return ImmutablePair.of(BAD_TOO_POOR, -1L);
        }
        if(payment < min) {
            return ImmutablePair.of(BAD_TOO_CHEAP, -1L);
        }
        if(payment > max) {
            return ImmutablePair.of(BAD_TOO_MUCH, -1L);
        }
        
        return ImmutablePair.of(OK, payment);
    }
    
    public final String getCurrencySymbol(final CommandContext ctx) {
        final GuildSettings settings = ctx.getSettings();
        final String c = settings.getCurrencySymbol();
        return c == null || c.isEmpty() ? CURRENCY_SYMBOL : c;
    }
    
    enum PaymentResult {
        OK,
        BAD_TOO_POOR,
        BAD_NOT_NUM,
        BAD_TOO_CHEAP,
        BAD_EMPTY,
        BAD_TOO_POOR_NO_BAL,
        BAD_TOO_MUCH,
    }
}
