package com.mewna.plugin.plugins.music;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author amy
 * @since 11/2/18.
 */
@Getter(onMethod_ = {@JsonProperty})
@Setter(onMethod_ = {@JsonProperty})
@Accessors(fluent = true)
@NoArgsConstructor
@AllArgsConstructor
public final class NekoTrackContext {
    @JsonProperty
    private String user;
    @JsonProperty
    private String channel;
    @JsonProperty
    private String guild;
}
