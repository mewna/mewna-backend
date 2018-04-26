package gg.cute.plugin.impl.abilities;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author amy
 * @since 4/25/18.
 */
public class TriggerTest {
    @Test
    public void getName() {
        assertEquals("Level Up", Trigger.LEVEL_UP.getName());
        assertEquals("Message Send", Trigger.MESSAGE_SEND.getName());
        assertEquals("Achievement Get", Trigger.ACHIEVEMENT_GET.getName());
    }
}