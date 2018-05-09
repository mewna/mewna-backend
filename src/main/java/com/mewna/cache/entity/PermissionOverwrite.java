package com.mewna.cache.entity;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
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
@Table(keyspace = "mewna", name = "overwrites")
public class PermissionOverwrite {
    @SuppressWarnings("DefaultAnnotationParam")
    @PartitionKey(0)
    private String channel;
    @PartitionKey(1)
    private String id;
    private String type;
    private long allow;
    private long deny;
}
