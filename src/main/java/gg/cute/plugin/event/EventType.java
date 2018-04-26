package gg.cute.plugin.event;

/**
 * Question: "WHY IS THIS NOT ENUM AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
 * Answer: "Working with strings was easier, ie less deserialization work"
 *
 * @author amy
 * @since 4/17/18.
 */
public interface EventType {
    // @formatter:off
    
    // Discord events
    String CHANNEL_CREATE               = "CHANNEL_CREATE";
    String CHANNEL_DELETE               = "CHANNEL_DELETE";
    String CHANNEL_UPDATE               = "CHANNEL_UPDATE";
    String GUILD_CREATE                 = "GUILD_CREATE";
    String GUILD_DELETE                 = "GUILD_DELETE";
    String GUILD_UPDATE                 = "GUILD_UPDATE";
    String GUILD_EMOJIS_UPDATE          = "GUILD_EMOJIS_UPDATE";
    String GUILD_MEMBER_ADD             = "GUILD_MEMBER_ADD";
    String GUILD_MEMBER_REMOVE          = "GUILD_MEMBER_REMOVE";
    String GUILD_MEMBER_UPDATE          = "GUILD_MEMBER_UPDATE";
    String GUILD_MEMBERS_CHUNK          = "GUILD_MEMBERS_CHUNK";
    String GUILD_ROLE_CREATE            = "GUILD_ROLE_CREATE";
    String GUILD_ROLE_DELETE            = "GUILD_ROLE_DELETE";
    String GUILD_ROLE_UPDATE            = "GUILD_ROLE_UPDATE";
    String USER_UPDATE                  = "USER_UPDATE";
    String VOICE_SERVER_UPDATE          = "VOICE_SERVER_UPDATE";
    String MESSAGE_CREATE               = "MESSAGE_CREATE";
    String MESSAGE_DELETE               = "MESSAGE_DELETE";
    String MESSAGE_DELETE_BULK          = "MESSAGE_DELETE_BULK";
    String MESSAGE_UPDATE               = "MESSAGE_UPDATE";
    String GUILD_SYNC                   = "GUILD_SYNC";
    String GUILD_BAN_ADD                = "GUILD_BAN_ADD";
    String GUILD_BAN_REMOVE             = "GUILD_BAN_REMOVE";
    String MESSAGE_REACTION_ADD         = "MESSAGE_REACTION_ADD";
    String MESSAGE_REACTION_REMOVE      = "MESSAGE_REACTION_REMOVE";
    String MESSAGE_REACTION_REMOVE_ALL  = "MESSAGE_REACTION_REMOVE_ALL";
    String PRESENCE_UPDATE              = "PRESENCE_UPDATE";
    String READY                        = "READY";
    String TYPING_START                 = "TYPING_START";
    String VOICE_STATE_UPDATE           = "VOICE_STATE_UPDATE";
    
    // Audio server events
    String AUDIO_TRACK_START    = "AUDIO_TRACK_START";
    String AUDIO_TRACK_STOP     = "AUDIO_TRACK_STOP";
    String AUDIO_TRACK_PAUSE    = "AUDIO_TRACK_PAUSE";
    String AUDIO_TRACK_QUEUE    = "AUDIO_TRACK_QUEUE";
    String AUDIO_TRACK_INVALID  = "AUDIO_TRACK_INVALID";
    String AUDIO_QUEUE_END      = "AUDIO_QUEUE_END";
    
    // @formatter:on
}
