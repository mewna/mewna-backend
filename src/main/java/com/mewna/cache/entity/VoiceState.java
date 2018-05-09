package com.mewna.cache.entity;

import lombok.Value;

/**
 * @author amy
 * @since 4/19/18.
 */
@Value
public class VoiceState {
    private Guild guild;
    private Channel channel;
    private User user;
}
