package com.mewna.cache.entity;

import lombok.*;

import java.util.List;

/**
 * @author amy
 * @since 4/16/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    private String guildId;
    private String id;
    private String nick;
    private String joinedAt;
    private List<String> roles;
}
