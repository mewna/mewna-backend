package gg.cute.plugin;

import gg.cute.cache.entity.Guild;
import gg.cute.data.Player;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static gg.cute.plugin.BasePlugin.PaymentResult.*;
import static org.junit.Assert.assertEquals;

/**
 * @author amy
 * @since 4/14/18.
 */
public class BasePluginTest {
    @Test
    public void checkPayment() {
        final BasePlugin base = new BasePlugin();
        final Guild fakeGuild = new Guild("267500017260953601", "fake guild", null, "128316294742147072",
                "us-east", 1234);
        final Map<String, Long> guildBalances = new HashMap<>();
        guildBalances.put(fakeGuild.getId(), 0L);
        final Player fakePlayer = new Player("128316294742147072", guildBalances, null, null, 0L);
        
        // No input
        assertEquals(BAD_EMPTY, base.checkPayment(fakeGuild, fakePlayer, "", 0, 100).left);
        assertEquals(BAD_EMPTY, base.checkPayment(fakeGuild, fakePlayer, null, 0, 100).left);
        
        // Not a number
        assertEquals(BAD_NOT_NUM, base.checkPayment(fakeGuild, fakePlayer, "3q98ouiykrhf", 0, 100).left);
        assertEquals(BAD_NOT_NUM, base.checkPayment(fakeGuild, fakePlayer, Long.MAX_VALUE + "1", 0, 100).left);
        
        // User has no monies
        assertEquals(BAD_TOO_POOR_NO_BAL, base.checkPayment(fakeGuild, fakePlayer, "1", 0, 100).left);
        
        // User doesn't have enough to cover the min charge
        fakePlayer.getGuildBalances().put(fakeGuild.getId(), 5L);
        assertEquals(BAD_TOO_POOR, base.checkPayment(fakeGuild, fakePlayer, "50", 6, 100).left);
        // User doesn't have enough to cover their own payment
        assertEquals(BAD_TOO_POOR, base.checkPayment(fakeGuild, fakePlayer, "50", 1, 100).left);
        
        // User didn't try to pay enough
        assertEquals(BAD_TOO_CHEAP, base.checkPayment(fakeGuild, fakePlayer, "1", 2, 100).left);
        
        // User tried to pay too much
        fakePlayer.getGuildBalances().put(fakeGuild.getId(), 1000L);
        assertEquals(BAD_TOO_MUCH, base.checkPayment(fakeGuild, fakePlayer, "1000", 1, 100).left);
        
        // All good!
        assertEquals(OK, base.checkPayment(fakeGuild, fakePlayer, "50", 1, 100).left);
    }
}