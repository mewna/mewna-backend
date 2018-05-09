package com.mewna.util;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author amy
 * @since 4/14/18.
 */
public final class Time {
    private static final List<TimeUnit> timeUnits = Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES,
            TimeUnit.SECONDS);
    
    private Time() {
    }
    
    public static String toHumanReadableDuration(final long millis) {
        final StringBuilder builder = new StringBuilder();
        long acc = millis;
        for (final TimeUnit timeUnit : timeUnits) {
            final long convert = timeUnit.convert(acc, TimeUnit.MILLISECONDS);
            if (convert > 0) {
                builder.append(convert).append(' ').append(timeUnit.name().toLowerCase().replaceAll("s$", "(s)")).append(", ");
                acc -= TimeUnit.MILLISECONDS.convert(convert, timeUnit);
            }
        }
        if(builder.length() > 2) {
            return builder.substring(0, builder.length() - 2);
        } else {
            return "now";
        }
    }
    
    public static long now() {
        return System.currentTimeMillis();
    }
}
