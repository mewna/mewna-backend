package com.mewna.cache.entity;

import lombok.*;

/**
 * TODO: Handle unavailable status...
 *
 * @author amy
 * @since 4/8/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Guild {
    private String id;
    private String name;
    private String icon;
    private String ownerId;
    private String region;
    private int memberCount;
}
