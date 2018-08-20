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
public class User {
    private String id;
    private String name;
    private String discriminator;
    private String avatar;
    private boolean bot;
    
    public String asMention() {
        return "<@" + id + '>';
    }
    
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
