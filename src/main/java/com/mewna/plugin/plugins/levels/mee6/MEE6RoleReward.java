package com.mewna.plugin.plugins.levels.mee6;

import lombok.*;

/**
 * @author amy
 * @since 1/8/19.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public final class MEE6RoleReward {
    private int rank;
    private MEE6Role role;
    
    @Getter
    @Setter
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @SuppressWarnings("WeakerAccess")
    @AllArgsConstructor
    public static final class MEE6Role {
        private int color;
        private boolean hoist;
        private String id;
        private boolean managed;
        private boolean mentionable;
        private String name;
        private int permissions;
        private String position;
    }
}
