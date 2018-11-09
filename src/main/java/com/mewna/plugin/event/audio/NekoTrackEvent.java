package com.mewna.plugin.event.audio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mewna.plugin.plugins.music.NekoTrack;
import io.vertx.core.json.JsonObject;
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
public class NekoTrackEvent {
    @JsonProperty
    private TrackEventType type;
    @JsonProperty
    private NekoTrack track;
    @JsonProperty
    private long ts = System.currentTimeMillis();
    
    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }
    
    public enum TrackEventType {
        /**
         * Track started playing
         */
        AUDIO_TRACK_START,
        /**
         * Track stopped playing
         */
        AUDIO_TRACK_STOP,
        /**
         * Track was paused
         */
        AUDIO_TRACK_PAUSE,
        /**
         * Track was queued
         */
        AUDIO_TRACK_QUEUE,
        /**
         * Track was invalid
         */
        AUDIO_TRACK_INVALID,
        /**
         * Request to fetch current track
         */
        AUDIO_TRACK_NOW_PLAYING,
        /**
         * Queue ended
         */
        AUDIO_QUEUE_END,
        /**
         * No matches for track load
         */
        AUDIO_TRACK_NO_MATCHES,
        /**
         * Many tracks loaded. Track count will be encoded in the track title.
         */
        AUDIO_TRACK_QUEUE_MANY,
    }
}
