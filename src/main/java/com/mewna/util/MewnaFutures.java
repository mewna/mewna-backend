package com.mewna.util;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author amy
 * @since 11/1/18.
 */
public final class MewnaFutures {
    private MewnaFutures() {
    }
    
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Void> allOf(final CompletionStage... futures) {
        final CompletableFuture[] arr = Arrays.stream(futures)
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(arr);
    }
    
    public static <T> T get(@Nonnull final CompletableFuture<T> future) {
        try {
            return future.getNow(null);
        } catch(final Exception e) {
            return null;
        }
    }
    
    public static <T> T get(@Nonnull final CompletionStage<T> future) {
        return get(future.toCompletableFuture());
    }
}
