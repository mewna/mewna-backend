package com.mewna.cache.entity;

import lombok.*;

/**
 * @author amy
 * @since 4/16/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionOverwrite {
    private String channel;
    private String id;
    private String type;
    private long allow;
    private long deny;
}
