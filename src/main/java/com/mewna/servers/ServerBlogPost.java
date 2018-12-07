package com.mewna.servers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import io.vertx.core.json.JsonObject;
import lombok.*;

import java.util.Set;

/**
 * @author amy
 * @since 11/17/18.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("server_blog_posts")
@GIndex({"id", "author", "guild"})
public class ServerBlogPost {
    public static final int MAX_TITLE_LENGTH = 128;
    public static final int MAX_POST_LENGTH = 10_000;
    
    /**
     * This is a snowflake and thus encodes the timestamp.
     */
    @PrimaryKey
    private String id;
    
    /**
     * The ACCOUNT ID of the user that created this post. Used for correctly
     * attributing posts and shit.
     */
    private String author;
    
    /**
     * The GUILD ID that this post was created for. Used for obvious things.
     */
    private String guild;
    
    /**
     * Plain-text string, 128 char max.
     */
    private String title;
    
    /**
     * Markdown string, 10k char max.
     */
    private String content;
    
    /**
     * Think like "upvotes" or "points." We don't allow people to downvote or
     * have other kinds of reactions (for a variety of reasons...), so this
     * will only ever count up.
     */
    private Set<BoopData> boops;
    
    /**
     * @return The number of people who booped it.
     */
    @JsonIgnore
    public int boops() {
        return boops == null ? 0 : boops.size();
    }
    
    @JsonIgnore
    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }
    
    public boolean validate() {
        return /* id != null && id.matches("\\d+")
                &&*/ author != null && author.matches("\\d+")
                && guild != null && guild.matches("\\d+")
                && title != null && title.length() >= 3 && title.length() <= MAX_TITLE_LENGTH
                && content != null && content.length() >= 100 && content.length() <= MAX_POST_LENGTH
                // Incoming post update data should never have this data anyway
                && boops.isEmpty();
    }
}
