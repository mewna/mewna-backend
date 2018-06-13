package com.mewna.data;

import lombok.AllArgsConstructor;

/**
 * @author amy
 * @since 6/13/18.
 */
@AllArgsConstructor
public class Webhook {
    private final String channel;
    private final String guild;
    private final String id;
    private final String secret;
}
