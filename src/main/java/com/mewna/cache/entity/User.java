package com.mewna.cache.entity;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.datastax.driver.mapping.annotations.Transient;
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
@Table(keyspace = "mewna", name = "users")
public class User {
    @PartitionKey
    private String id;
    private String name;
    private String discriminator;
    private String avatar;
    private boolean bot;
    
    @Transient
    public String asMention() {
        return "<@" + id + '>';
    }
    
    @Transient
    public String getAvatarURL() {
        if(avatar != null && !avatar.trim().isEmpty()) {
            final String extension;
            if(avatar.startsWith("a_")) {
                extension = "gif";
            } else {
                extension = "png";
            }
            return String.format("https://cdn.discordapp.com/avatars/%s/%s.%s", id, avatar, extension);
        } else {
            final int mod = Integer.parseInt(discriminator != null ? discriminator : "0") % 5;
            return String.format("https://cdn.discordapp.com/embed/avatars/%s.png", mod);
        }
    }
}
