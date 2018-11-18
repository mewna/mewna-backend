package com.mewna.servers;

import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author amy
 * @since 11/17/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("server_blog_posts")
@GIndex({"id", "author"})
public class ServerBlogPost {
    public static final int MAX_POST_LENGTH = 10_000;
    
    @PrimaryKey
    private String id;
    
    /**
     * The ACCOUNT ID of the user that created this post. Used for correctly
     * attributing posts and shit.
     */
    private String author;
    
    /**
     * Markdown string, 10k char max.
     */
    private String content;
}
