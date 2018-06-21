package com.mewna.plugin.event.plugin.twitch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * @author amy
 * @since 6/20/18.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TwitchStreamer {
    private String id;
    private String login;
    @JsonProperty("display_name")
    private String displayName;
    private String type;
    @JsonProperty("view_count")
    private int viewCount;
    private String description;
    @JsonProperty("broadcaster_type")
    private String broadcasterType;
    @JsonProperty("offline_image_url")
    private String offlineImageUrl;
    @JsonProperty("profile_image_url")
    private String profileImageUrl;
}
