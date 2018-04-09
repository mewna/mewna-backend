package gg.cute.cache.entity;

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
@Builder(toBuilder = true)
@Table(keyspace = "cute", name = "guilds")
public class Guild {
    @PartitionKey
    private String id;
    private String name;
    private String icon;
    private String ownerId;
    private String region;
    private int memberCount;
}
