package com.mewna.plugin.event.plugin.twitch;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * @author amy
 * @since 6/20/18.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class TwitchStreamData {
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("community_ids")
    private List<String> communityIds;
    @JsonProperty("started_at")
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant startedAt;
    private String language;
    private String id;
    @JsonProperty("viewer_count")
    private int viewerCount;
    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
    private String title;
    private String type;
    @JsonProperty("game_id")
    private String gameId;
}
