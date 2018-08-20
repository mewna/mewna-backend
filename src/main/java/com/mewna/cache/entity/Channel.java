package com.mewna.cache.entity;

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
public class Channel {
    private String id;
    private int type;
    private String guildId;
    private String name;
    private boolean nsfw;
    
    public String asMention() {
        return "<#" + id + '>';
    }
}
