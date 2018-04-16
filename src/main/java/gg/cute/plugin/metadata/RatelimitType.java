package gg.cute.plugin.metadata;

/**
 * @author amy
 * @since 4/15/18.
 */
public enum RatelimitType {
    /**
     * Ratelimit is applied to the user everywhere
     */
    GLOBAL,
    /**
     * Ratelimit is applied to the user only in the specific channel
     */
    CHANNEL,
    /**
     * Ratelimit is applied to the user only in the specific guild
     */
    GUILD,
}
