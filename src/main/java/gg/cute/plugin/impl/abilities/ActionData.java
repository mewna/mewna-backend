package gg.cute.plugin.impl.abilities;

import lombok.Value;

/**
 * @author amy
 * @since 4/25/18.
 */
@Value
public class ActionData<T> {
    private Action type;
    private T data;
}
