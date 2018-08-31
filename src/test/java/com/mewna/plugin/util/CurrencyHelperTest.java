package com.mewna.plugin.util;

import com.mewna.data.Player;
import org.junit.Test;

import java.util.HashMap;

import static com.mewna.plugin.util.CurrencyHelper.PaymentResult.*;
import static org.junit.Assert.assertEquals;

/**
 * @author amy
 * @since 4/15/18.
 */
public class CurrencyHelperTest {
    @Test
    public void testCheckPayment() {
        final CurrencyHelper base = new CurrencyHelper();
        final Player fakePlayer = new Player("128316294742147072", 0L, 0L, 0L, new HashMap<>(),
                0L, new HashMap<>(), new HashMap<>(), null, null);
        
        // No input
        assertEquals(BAD_EMPTY, base.checkPayment(fakePlayer, "", 0, 100).left);
        assertEquals(BAD_EMPTY, base.checkPayment(fakePlayer, null, 0, 100).left);
        
        // Not a number
        assertEquals(BAD_NOT_NUM, base.checkPayment(fakePlayer, "3q98ouiykrhf", 0, 100).left);
        assertEquals(BAD_NOT_NUM, base.checkPayment(fakePlayer, Long.MAX_VALUE + "1", 0, 100).left);
        
        // User has no monies
        assertEquals(BAD_TOO_POOR_NO_BAL, base.checkPayment(fakePlayer, "1", 0, 100).left);
        
        // User doesn't have enough to cover the min charge
        fakePlayer.setBalance(5L);
        assertEquals(BAD_TOO_POOR, base.checkPayment(fakePlayer, "50", 6, 100).left);
        // User doesn't have enough to cover their own payment
        assertEquals(BAD_TOO_POOR, base.checkPayment(fakePlayer, "50", 1, 100).left);
        
        // User didn't try to pay enough
        assertEquals(BAD_TOO_CHEAP, base.checkPayment(fakePlayer, "1", 2, 100).left);
        
        // User tried to pay too much
        fakePlayer.setBalance(1000L);
        assertEquals(BAD_TOO_MUCH, base.checkPayment(fakePlayer, "1000", 1, 100).left);
        
        // All good!
        assertEquals(OK, base.checkPayment(fakePlayer, "50", 1, 100).left);
    }
}