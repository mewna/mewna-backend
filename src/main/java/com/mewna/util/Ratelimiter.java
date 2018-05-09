package com.mewna.util;

import com.mewna.Cute;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * @author amy
 * @since 4/14/18.
 */
@SuppressWarnings("unused")
public class Ratelimiter {
    private final Cute cute;
    
    public Ratelimiter(final Cute cute) {
        this.cute = cute;
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
            cute.getDatabase().redis(j -> {
                final String key = id + ':' + type + ":ratelimit";
                j.set(key, String.valueOf(System.currentTimeMillis()));
            });
        }
        return ratelimited;
    }
    
    @SuppressWarnings("unchecked")
    private ImmutablePair<Boolean, Long> isRatelimited(final String id, final String type, final long ms) {
        final ImmutablePair[] pair = {null};
        
        cute.getDatabase().redis(j -> {
            final String key = id + ':' + type + ":ratelimit";
            if(j.exists(key)) {
                final String v = j.get(key);
                try {
                    final long last = Long.parseLong(v);
                    final long now = System.currentTimeMillis();
                    if(last + ms > now) {
                        pair[0] = new ImmutablePair<>(true, last + ms - now);
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