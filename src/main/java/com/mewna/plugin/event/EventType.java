package com.mewna.plugin.event;

/**
 * Question: "WHY IS THIS NOT ENUM AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
 * Answer: "Working with strings was easier, ie less deserialization work"
 *
 * @author amy
 * @since 4/17/18.
 */
public interface EventType {
    // @formatter:off
    
    // Audio server events
    String AUDIO_TRACK_START        = "AUDIO_TRACK_START";
    String AUDIO_TRACK_STOP         = "AUDIO_TRACK_STOP";
    String AUDIO_TRACK_PAUSE        = "AUDIO_TRACK_PAUSE";
    String AUDIO_TRACK_QUEUE        = "AUDIO_TRACK_QUEUE";
    String AUDIO_TRACK_INVALID      = "AUDIO_TRACK_INVALID";
    String AUDIO_TRACK_NOW_PLAYING  = "AUDIO_TRACK_NOW_PLAYING";
    String AUDIO_QUEUE_END          = "AUDIO_QUEUE_END";
    String AUDIO_TRACK_NO_MATCHES   = "AUDIO_TRACK_NO_MATCHES";
    String AUDIO_TRACK_QUEUE_MANY   = "AUDIO_TRACK_QUEUE_MANY";
    
    
    // Internal events
    String LEVEL_UP = "LEVEL_UP";
    String PLAYER_EVENT = "PLAYER_EVENT";
    String ACCOUNT_EVENT = "ACCOUNT_EVENT";
    String SERVER_EVENT = "SERVER_EVENT";
    
    // Telepathy events
    String TWITCH_STREAM_START = "TWITCH_STREAM_START";
    String TWITCH_STREAM_END = "TWITCH_STREAM_END";
    String TWITCH_FOLLOWER = "TWITCH_FOLLOWER";
    
    // @formatter:on
}
