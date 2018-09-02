package com.mewna.util;

import com.mewna.Mewna;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author amy
 * @since 4/14/18.
 */
@SuppressWarnings("unused")
public class Ratelimiter {
    private final Mewna mewna;
    
    public Ratelimiter(final Mewna mewna) {
        this.mewna = mewna;
    }
    
    /**
     * Check if the id is ratelimited. If the id is NOT ratelimited, update it
     * to the current time and allow it to happen.
     *
     * @param id   The ID to check against
     * @param type The type of the ratelimit
     * @param ms   How long the ratelimit should be, in milliseconds
     *
     * @return A (isRatelimited, timeLeft) tuple
     */
    public ImmutablePair<Boolean, Long> checkUpdateRatelimit(final String id, final String type, final long ms) {
        final ImmutablePair<Boolean, Long> ratelimited = isRatelimited(id, type, ms);
        if(!ratelimited.left) {
            mewna.getDatabase().redis(j -> {
                final String key = id + ':' + type + ":ratelimit";
                j.set(key, String.valueOf(System.currentTimeMillis()));
            });
        }
        return ratelimited;
    }
    
    public long getRatelimitTime(final String id, final String type, final long ms) {
        final long[] res = {0};
        mewna.getDatabase().redis(j -> {
            final String key = id + ':' + type + ":ratelimit";
            if(j.exists(key)) {
                final String v = j.get(key);
                try {
                    final long last = Long.parseLong(v);
                    final long now = System.currentTimeMillis();
                    res[0] = last - now + ThreadLocalRandom.current().nextLong(5000L);
                } catch(final Exception ignored) {}
            }
        });
        return res[0];
    }
    
    @SuppressWarnings("unchecked")
    private ImmutablePair<Boolean, Long> isRatelimited(final String id, final String type, long ms) {
        ms += ThreadLocalRandom.current().nextLong(5000L);
        final ImmutablePair[] pair = {null};
    
        final long finalMs = ms;
        mewna.getDatabase().redis(j -> {
            final String key = id + ':' + type + ":ratelimit";
            if(j.exists(key)) {
                final String v = j.get(key);
                try {
                    final long last = Long.parseLong(v);
                    final long now = System.currentTimeMillis();
                    if(last + finalMs > now) {
                        pair[0] = new ImmutablePair<>(true, last + finalMs - now );
                    } else {
                        pair[0] = new ImmutablePair<>(false, -1L);
                    }
                } catch(final Exception ignored) {
                    pair[0] = new ImmutablePair<>(false, -1L);
                }
            } else {
                pair[0] = new ImmutablePair<>(false, -1L);
            }
        });
        
        // This is a safe cast
        return (ImmutablePair<Boolean, Long>) pair[0];
    }
}