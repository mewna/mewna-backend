package com.mewna.accounts.timeline;

import com.mewna.plugin.util.Snowflakes;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Value;

/**
 * @author amy
 * @since 6/25/18.
 */
@Value
@Table("timeline_posts")
@GIndex({"id", "author"})
public class TimelinePost {
    @PrimaryKey
    private final String id;
    
    /**
     * Note: As of right now, this may refer to a {@link com.mewna.data.Player}
     * OR an {@link com.mewna.accounts.Account}, due to the unfortunate facts
     * about how things really work.
     */
    private final String author;
    
    /**
     * If a post is marked as {@code system}, the {@link #text} field is NOT
     * interpreted as user-created text, but rather as a translation string of
     * sorts that should be "translated" and then formatted with the correct
     * data.
     */
    private final boolean system;
    private final String text;
    
    public static TimelinePost create(final String author, final boolean system, final String text) {
        return new TimelinePost(Snowflakes.getNewSnowflake(), author, system, text);
    }
}
