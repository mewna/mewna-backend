package gg.cute.data.config;

import gg.cute.data.GuildSettings;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author amy
 * @since 5/9/18.
 */
public class ConfigVerifierTest {
    @Test
    public void verify() {
        final ConfigVerifier verifier = new ConfigVerifier();
        // Verify base settings
        {
            final GuildSettings base = GuildSettings.base("267500017260953601");
            final Map<String, List<String>> res = verifier.verify(GuildSettings.class, new JSONObject(base));
            /*
            if(!res.isEmpty()) {
                StringBuilder sb = new StringBuilder("VERIFICATION FAILED:\n");
                res.forEach((k, v) -> sb.append(k).append(" - ").append(String.join(", ", v)).append('\n'));
                System.err.println(sb);
            }
            */
            assertTrue(res.isEmpty());
        }
        // Verify single boolean
        {
            final Map<String, List<String>> res = verifier.verify(GuildSettings.class, new JSONObject().put("levelsEnabled", false));
            assertTrue(res.isEmpty());
        }
        // Verify string and STRING_LEN_8
        {
            Map<String, List<String>> res = verifier.verify(GuildSettings.class, new JSONObject().put("customPrefix", "!"));
            assertTrue(res.isEmpty());
            res = verifier.verify(GuildSettings.class, new JSONObject().put("customPrefix", "!!!!!!!!"));
            assertTrue(res.isEmpty());
            res = verifier.verify(GuildSettings.class, new JSONObject().put("customPrefix", "!!!!!!!!!!!!!!!!"));
            assertFalse(res.isEmpty());
        }
        // Verify bad type
        {
            Map<String, List<String>> res = verifier.verify(GuildSettings.class, new JSONObject().put("levelsEnabled", "9327845"));
            assertFalse(res.isEmpty());
            res = verifier.verify(GuildSettings.class, new JSONObject().put("customPrefix", true));
            assertFalse(res.isEmpty());
            res = verifier.verify(GuildSettings.class, new JSONObject().put("customPrefix", 283764));
            assertFalse(res.isEmpty());
            res = verifier.verify(GuildSettings.class, new JSONObject().put("commandSettings", JSONObject.NULL));
            assertFalse(res.isEmpty());
        }
    }
    
    @Test
    public void update() {
        final ConfigVerifier verifier = new ConfigVerifier();
        final GuildSettings base = GuildSettings.base("267500017260953601");
        final JSONObject update = new JSONObject().put("customPrefix", "!");
        assertEquals(null, base.getCustomPrefix());
        Map<String, List<String>> res = verifier.verify(GuildSettings.class, new JSONObject(base));
        assertTrue(res.isEmpty());
        res = verifier.verify(GuildSettings.class, update);
        assertTrue(res.isEmpty());
        verifier.update(base, update);
        assertEquals("!", base.getCustomPrefix());
    }
}