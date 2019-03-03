package com.mewna.util;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Stuff to help with moving off of vert.x threads because lol v.x gay :^)
 *
 * @author amy
 * @since 2/10/19.
 */
public final class Async {
    private static final ExecutorService POOL = Executors.newCachedThreadPool();
    // Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    
    private Async() {
    }
    
    /**
     * Moves the task into a pooled thread. These threads are safe to block
     * inside of.
     *
     * @param task The task to move.
     */
    public static void move(@Nonnull final Runnable task) {
        POOL.execute(task);
    }
    
    /**
     * Move the task into a pooled thread, and return a future that can be
     * completed with a result. These threads are safe to block inside of.
     *
     * @param task The task to move.
     * @param <T>  The type of the value being returned.
     *
     * @return A future that completes with the value produced by the task.
     */
    public static <T> CompletionStage<T> move(@Nonnull final Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, POOL);
    }
}
