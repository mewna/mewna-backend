package gg.cute.plugin.event.audio;

import gg.cute.cache.entity.Channel;
import gg.cute.cache.entity.Guild;
import gg.cute.cache.entity.User;
import gg.cute.plugin.event.BaseEvent;
import gg.cute.plugin.event.EventType;
import gg.cute.plugin.impl.audio.AudioTrackInfo;
import lombok.Getter;

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
        QUEUE_END,
    }
}
