package com.mewna.cache.entity;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
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
@Table(keyspace = "mewna", name = "members")
public class Member {
    @SuppressWarnings("DefaultAnnotationParam")
    @PartitionKey(0)
    private String guildId;
    @PartitionKey(1)
    private String id;
    private String nick;
    private String joinedAt;
    private List<String> roles;
}
