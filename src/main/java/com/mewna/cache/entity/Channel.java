package com.mewna.cache.entity;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
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
@Table(keyspace = "mewna", name = "channels")
public class Channel {
    @PartitionKey
    private String id;
    private int type;
    private String guildId;
    private String name;
    private boolean nsfw;
    
    public String asMention() {
        return "<#" + id + '>';
    }
}
