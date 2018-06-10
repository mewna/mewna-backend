package com.mewna.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author amy
 * @since 6/3/18.
 */
public class NumbersTest {
    
    /*
     * https://stackoverflow.com/questions/4753251/how-to-go-about-formatting-1200-to-1-2k-in-java
     */
    @Test
    public void format() {
        final long[] numbers = {0, 5, 999, 1_000, -5_821, 10_500, -101_800, 2_000_000, -7_800_000, 92_150_000, 123_200_000,
                9_999_999, 999_999_999_999_999_999L, 1_230_000_000_000_000L, Long.MIN_VALUE, Long.MAX_VALUE};
        final String[] expected = {"0", "5", "999", "1K", "-5.8K", "10K", "-101K", "2M", "-7.8M", "92M", "123M", "9.9M",
                "999P", "1.2P", "-9.2E", "9.2E"};
        for(int i = 0; i < numbers.length; i++) {
            final long n = numbers[i];
            final String formatted = Numbers.format(n);
            assertEquals(expected[i], formatted);
        }
    }
}