package com.mewna.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * @author amy
 * @since 1/7/19.
 */
@Accessors(fluent = true)
public class Profiler {
    @Getter
    private final Deque<ProfilerSection> sections = new ConcurrentLinkedDeque<>();
    
    public Profiler(final String startName, final long start) {
        sections.addLast(new ProfilerSection(startName, start));
    }
    
    public void section(final String name) {
        sections.peekLast().end(System.currentTimeMillis());
        sections.addLast(new ProfilerSection(name, System.currentTimeMillis()));
    }
    
    public void end() {
        sections.peekLast().end(System.currentTimeMillis());
    }
    
    @Accessors(fluent = true)
    @Getter
    @RequiredArgsConstructor
    public static final class ProfilerSection {
        private final String name;
        private final long start;
        @Setter
        private long end;
    }
}
