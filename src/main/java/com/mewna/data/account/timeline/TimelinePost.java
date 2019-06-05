package com.mewna.data.account.timeline;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mewna.data.posts.Post;
import com.mewna.plugin.util.Snowflakes;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import io.vertx.core.json.JsonObject;
import lombok.*;

import java.util.Map;

/**
 * @author amy
 * @since 6/25/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("timeline_posts")
@GIndex({"id", "author"})
public class TimelinePost {
    @PrimaryKey
    private String id;
    
    /**
     * In the case of server-specific posts, the "author" is the guild id, and
     * the real author is encoded in the {@link #content}
     * {@link PostContent#body}.
     */
    @JsonProperty("author")
    private String author;
    
    /**
     * If a post is marked as {@code system}, the {@link #content} field is NOT
     * interpreted as user-created text, but rather as a blob of JSON that
     * should be parsed to work out display etc. info.
     */
    private boolean system;
    
    // tfw no unions feels bad man
    // So we only SOMETIMES want this to be post. When it's a `system` post, we
    // want this to be proper structured JSON that doesn't get serialized to a
    // string so that we can do some queries over it and stuff
    // V:
    private PostContent content;
    
    public static TimelinePost create(final String author, final boolean system, final String text) {
        return new TimelinePost(Snowflakes.getNewSnowflake(), author, system,
                system ? new PostContent(null, new JsonObject(text).getMap())
                        : new PostContent(new JsonObject(text).mapTo(Post.class), null));
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuppressWarnings("WeakerAccess")
    public static final class PostContent {
        @JsonProperty("text") // Mistakes were made
        private Post body;
        private Map<String, Object> data;
    }
}
