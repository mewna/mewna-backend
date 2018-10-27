package com.mewna.plugin.util;

import com.mewna.Mewna;
import com.mewna.data.Player;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.plugins.settings.EconomySettings;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.inject.Inject;

import static com.mewna.plugin.util.CurrencyHelper.PaymentResult.*;
import static com.mewna.util.Translator.$;

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
        final ImmutablePair<PaymentResult, Long> check = checkPayment(ctx.getPlayer(), maybeAmount, min, max);
        final String symbol = getCurrencySymbol(ctx);
        switch(check.left) {
            case BAD_EMPTY: {
                mewna.getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                        $(ctx.getLanguage(), "plugins.economy.commands.empty-payment"));
                return ImmutablePair.of(false, -1L);
            }
            case BAD_NOT_NUM: {
                mewna.getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                        $(ctx.getLanguage(), "plugins.economy.commands.payment-not-number")
                                .replace("$number", maybeAmount));
                return ImmutablePair.of(false, -1L);
            }
            case BAD_TOO_POOR_NO_BAL: {
                mewna.getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                        $(ctx.getLanguage(), "poor-no-money"));
                return ImmutablePair.of(false, -1L);
            }
            case BAD_TOO_POOR: {
                mewna.getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                        $(ctx.getLanguage(), "poor-not-enough")
                            .replace("$amount", maybeAmount)
                        .replace("$balance", ctx.getPlayer().getBalance()+"")
                        .replace("$symbol", symbol));
                return ImmutablePair.of(false, -1L);
            }
            case BAD_TOO_CHEAP: {
                mewna.getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                        $(ctx.getLanguage(), "too-cheap")
                        .replace("$amount", maybeAmount)
                        .replace("$symbol", symbol)
                        .replace("$minAmount", min+""));
                return ImmutablePair.of(false, -1L);
            }
            case BAD_TOO_MUCH: {
                mewna.getCatnip().rest().channel().sendMessage(ctx.getChannel().getId(),
                        $(ctx.getLanguage(), "too-much")
                                .replace("$amount", maybeAmount)
                                .replace("$symbol", symbol)
                                .replace("$maxAmount", max+""));
                return ImmutablePair.of(false, -1L);
            }
            case OK: {
                mewna.getStatsClient().count("discord.backend.money.spent", check.right);
                ctx.getPlayer().incrementBalance(-check.right);
                mewna.getDatabase().savePlayer(ctx.getPlayer());
                return ImmutablePair.of(true, check.right);
            }
            default: {
                throw new IllegalStateException("Got invalid payment result state: " + check.left + "!?");
            }
        }
    }
    
    @SuppressWarnings("WeakerAccess")
    public final ImmutablePair<PaymentResult, Long> checkPayment(final Player player, final String maybeAmount,
                                                                 final long min, final long max) {
        final long balance = player.getBalance();
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
        final String c = mewna.getDatabase().getOrBaseSettings(EconomySettings.class, ctx.getGuild().getId()).getCurrencySymbol();
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
