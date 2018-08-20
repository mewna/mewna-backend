package com.mewna.cache.entity;

import lombok.*;

/**
 * @author amy
 * @since 4/8/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    private String id;
    private String name;
    private int color;
    private String guildId;
    private boolean managed;
}
