package com.mewna.servers;

import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * @author infinity
 * @since 11/28/18
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("boop_data")
@GIndex({"id", "user", "timestamp"})
public class BoopData {
    @PrimaryKey
    private String id;
    
    private String user;
    
    private Instant timestamp;
    
    public OffsetDateTime odt() {
        // I'm questioning my sanity right now.
        return timestamp.atOffset(ZoneOffset.UTC).toZonedDateTime().toOffsetDateTime();
    }
}
