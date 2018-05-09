package com.mewna.plugin.impl;

import org.junit.Test;

import static org.junit.Assert.*;
import static com.mewna.plugin.impl.PluginLevels.*;

/**
 * @author amy
 * @since 4/17/18.
 */
public class PluginLevelsTest {
    @Test
    public void testLevelToXp() {
        assertEquals(0, levelToXp(0));
        assertEquals(100, levelToXp(1));
        assertEquals(220, levelToXp(2));
    }
    
    @Test
    public void testFullLevelToXp() {
        assertEquals(0, fullLevelToXp(0));
        assertEquals(100, fullLevelToXp(1));
        assertEquals(320, fullLevelToXp(2));
    }
    
    @Test
    public void testXpToLevel() {
        assertEquals(0, xpToLevel(0));
        assertEquals(1, xpToLevel(100));
        assertEquals(1, xpToLevel(220));
        assertEquals(2, xpToLevel(320));
    }
    
    @Test
    public void testIsLevelUp() {
        assertFalse(isLevelUp(0, 99));
        assertTrue(isLevelUp(99, 100));
        assertFalse(isLevelUp(100, 220));
        assertTrue(isLevelUp(220, 320));
    }
}