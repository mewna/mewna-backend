package com.mewna.plugin.plugins.levels.mee6;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * @author amy
 * @since 1/8/19.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public final class MEE6Player {
    private String avatar;
    @JsonProperty("detailed_xp")
    private List<Integer> detailedXp;
    private String discriminator;
    @JsonProperty("guild_id")
    private String guildId;
    private String id;
    private String level;
    private String username;
    private String xp;
}
