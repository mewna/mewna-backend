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
@Builder
@Table(keyspace = "cute", name = "users")
public class User {
    @PartitionKey
    private String id;
    private String name;
    private String discriminator;
    private String avatar;
    private boolean bot;
    
    public String asMention() {
        return "<@" + id + '>';
    }
}
