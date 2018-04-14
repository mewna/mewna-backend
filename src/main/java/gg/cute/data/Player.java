package gg.cute.data;

import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Value;

/**
 * @author amy
 * @since 4/10/18.
 */
@Value
@Table("players")
public class Player {
    @PrimaryKey
    private String id;
    
    private long flowers;
    
    private long petals;
}
