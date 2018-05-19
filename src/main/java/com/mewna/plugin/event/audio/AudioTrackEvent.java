package com.mewna.plugin.event.audio;

import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;
import lombok.Value;
import org.json.JSONObject;

/**
 * @author amy
 * @since 4/22/18.
 */
@Getter
public class AudioTrackEvent extends BaseEvent {
    private final TrackMode trackMode;
    private final Guild guild;
    private final Channel channel;
    private final User user;
    private final AudioTrackInfo info;
    
    public AudioTrackEvent(final TrackMode trackMode, final Guild guild, final Channel channel, final User user,
                           final AudioTrackInfo info) {
        super(EventType.AUDIO_TRACK_START);
        this.trackMode = trackMode;
        this.guild = guild;
        this.channel = channel;
        this.user = user;
        this.info = info;
    }
    
    public enum TrackMode {
        TRACK_START,
        TRACK_STOP,
        TRACK_QUEUE,
        TRACK_INVALID,
        TRACK_PAUSE,
        TRACK_NOW_PLAYING,
        QUEUE_END,
    }
    
    @Value
    public static class AudioTrackInfo {
        private String identifier;
        private String author;
        private int length;
        private boolean stream;
        private String title;
        private String uri;
        
        public static AudioTrackInfo fromJson(JSONObject o) {
            return new AudioTrackInfo(o.getString("identifier"), o.getString("author"), o.getInt("length"),
                    o.getBoolean("isStream"), o.getString("title"), o.getString("uri"));
        }
    }
}
