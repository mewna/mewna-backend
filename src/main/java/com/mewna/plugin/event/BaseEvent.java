package com.mewna.plugin.event;

import lombok.RequiredArgsConstructor;

/**
 * Base internal Discord event data class. Children of this class are expected
 * to also be pure data classes.
 *
 * @author amy
 * @since 4/16/18.
 */
@RequiredArgsConstructor
public abstract class BaseEvent {
    private final String type;
}
