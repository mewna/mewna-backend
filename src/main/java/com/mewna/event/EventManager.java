package com.mewna.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.mewna.Mewna;
import com.mewna.cache.DiscordCache;
import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.nats.SocketEvent;
import com.mewna.plugin.event.audio.AudioTrackEvent;
import com.mewna.plugin.event.audio.AudioTrackEvent.AudioTrackInfo;
import com.mewna.plugin.event.guild.member.GuildMemberAddEvent;
import com.mewna.plugin.event.guild.member.GuildMemberRemoveEvent;
import com.mewna.plugin.event.message.MessageDeleteBulkEvent;
import com.mewna.plugin.event.message.MessageDeleteEvent;
import com.mewna.plugin.event.plugin.levels.LevelUpEvent;
import com.mewna.plugin.event.plugin.twitch.*;
import lombok.Getter;
import net.dv8tion.jda.core.entities.MessageType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;

import static com.mewna.plugin.event.EventType.*;
import static com.mewna.plugin.event.audio.AudioTrackEvent.TrackMode;

// TODO: Probably should consider refactoring this out into smaller modules...
/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings({"unused", "OverlyCoupledClass"})
public class EventManager {
    private static final ObjectMapper MAPPER;
    
    static {
        MAPPER = new ObjectMapper();
        MAPPER.registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module());
    }
    
    @Getter
    private final Mewna mewna;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private final DiscordCache cache;
    private final Map<String, BiConsumer<SocketEvent, JSONObject>> handlers = new HashMap<>();
    
    public EventManager(final Mewna mewna) {
        this.mewna = mewna;
        cache = new DiscordCache(mewna);
        
        // Channels
        handlers.put(CHANNEL_CREATE, (event, data) -> {
            // TODO: DMs always send a MESSAGE_CREATE *and* a CHANNEL_CREATE - we need to be prepared for both of those.
            cache.cacheChannel(data);
        });
        handlers.put(CHANNEL_DELETE, (event, data) -> cache.deleteChannel(data.getString("id")));
        handlers.put(CHANNEL_UPDATE, (event, data) -> cache.cacheChannel(data));
        
        // Guilds
        handlers.put(GUILD_CREATE, (event, data) -> {
            final String id = data.getString("id");
            cache.cacheGuild(data);
            final JSONArray roles = data.getJSONArray("roles");
            final JSONArray members = data.getJSONArray("members");
            final JSONArray channels = data.getJSONArray("channels");
            channels.forEach(r -> {
                final JSONObject o = (JSONObject) r;
                cache.cacheChannel(o.put("guild_id", id));
            });
            roles.forEach(r -> {
                // We do this because ROLE_* events give us {role: {}, guild_id: ""}
                // and it's easier to just have one method and transform data before
                final JSONObject o = (JSONObject) r;
                cache.cacheRole(new JSONObject().put("guild_id", id).put("role", o));
            });
            logger.info("Caching initial {} member chunk.", members.length());
            members.forEach(r -> {
                cache.cacheUser(((JSONObject) r).getJSONObject("user"));
                cache.cacheMember(id, (JSONObject) r);
            });
            if(data.has("voice_states") && !data.isNull("voice_states")) {
                final JSONArray states = data.getJSONArray("voice_states");
                for(final Object o : states) {
                    final JSONObject state = (JSONObject) o;
                    state.put("guild_id", id);
                    // TODO: Bulk-cache in redis transactions
                    cache.cacheVoiceState(state);
                }
            }
        });
        handlers.put(GUILD_DELETE, (event, data) -> {
            if(data.has("unavailable")) {
                logger.warn("Guild went unavailable: {}", data.getString("id"));
            } else {
                cache.deleteGuild(data.getString("id"));
            }
        });
        handlers.put(GUILD_UPDATE, (event, data) -> cache.cacheGuild(data));
        
        // Emotes
        // TODO: Do I REALLY care?
        handlers.put(GUILD_EMOJIS_UPDATE, (event, data) -> {
        });
        
        // Members
        handlers.put(GUILD_MEMBER_ADD, (event, data) -> {
            final JSONObject user = data.getJSONObject("user");
            cache.cacheUser(user);
            cache.cacheMember(data.getString("guild_id"), data);
            final Guild guild = cache.getGuild(data.getString("guild_id"));
            cache.getMappingManager().mapper(Guild.class).save(guild.toBuilder().memberCount(guild.getMemberCount() + 1).build());
            mewna.getPluginManager().processEvent(event.getType(),
                    new GuildMemberAddEvent(guild, cache.getMember(guild, cache.getUser(user.getString("id")))));
        });
        handlers.put(GUILD_MEMBER_REMOVE, (event, data) -> {
            final Guild guild = cache.getGuild(data.getString("guild_id"));
            cache.deleteMember(data.getString("guild_id"), data.getJSONObject("user").getString("id"));
            cache.getMappingManager().mapper(Guild.class).save(guild.toBuilder().memberCount(guild.getMemberCount() - 1).build());
            mewna.getPluginManager().processEvent(event.getType(), new GuildMemberRemoveEvent(guild,
                    cache.getUser(data.getJSONObject("user").getString("id"))));
        });
        handlers.put(GUILD_MEMBER_UPDATE, (event, data) -> {
            final String guildId = data.getString("guild_id");
            cache.cacheMember(guildId, data);
        });
        handlers.put(GUILD_MEMBERS_CHUNK, (event, data) -> {
            final JSONArray members = data.getJSONArray("members");
            logger.info("Caching chunk with {} members.", members.length());
            members.forEach(m -> {
                cache.cacheUser(((JSONObject) m).getJSONObject("user"));
                cache.cacheMember(data.getString("guild_id"), (JSONObject) m);
            });
        });
        
        // Roles
        handlers.put(GUILD_ROLE_CREATE, (event, data) -> cache.cacheRole(data));
        handlers.put(GUILD_ROLE_DELETE, (event, data) -> cache.deleteRole(data.getString("role_id")));
        handlers.put(GUILD_ROLE_UPDATE, (event, data) -> cache.cacheRole(data));
        
        // Users
        handlers.put(USER_UPDATE, (event, data) -> cache.cacheUser(data));
        
        // Voice
        handlers.put(VOICE_SERVER_UPDATE, (event, data) -> {
            // Not needed
        });
        handlers.put(VOICE_STATE_UPDATE, (event, data) -> cache.cacheVoiceState(data));
        
        // Messages
        handlers.put(MESSAGE_CREATE, (event, data) -> {
            // If it's a webhook ID, **or** it's not a DEFAULT message, ignore it.
            // Possible message types:
            //
            // TYPE                     ID
            // ---------------------------
            // DEFAULT                  0
            // RECIPIENT_ADD            1
            // RECIPIENT_REMOVE         2
            // CALL                     3
            // CHANNEL_NAME_CHANGE      4
            // CHANNEL_ICON_CHANGE      5
            // CHANNEL_PINNED_MESSAGE   6
            // GUILD_MEMBER_JOIN        7
            if(!data.isNull("webhook_id") || data.getInt("type") != MessageType.DEFAULT.getId()) {
                return;
            }
            // This will pass down to the event handler, so we don't need to worry about it here
            mewna.getCommandManager().tryExecCommand(data);
        });
        handlers.put(MESSAGE_DELETE, (event, data) -> {
            // Would have to cache messages...
            mewna.getPluginManager().processEvent(event.getType(), new MessageDeleteEvent(data.getString("id"),
                    cache.getChannel(data.getString("channel_id"))));
        });
        handlers.put(MESSAGE_DELETE_BULK, (event, data) -> {
            // Would have to cache messages...
            final JSONArray jsonArray = data.getJSONArray("ids");
            final List<String> list = new ArrayList<>();
            jsonArray.forEach(e -> list.add((String) e));
            mewna.getPluginManager().processEvent(event.getType(),
                    new MessageDeleteBulkEvent(cache.getChannel(data.getString("channel_id")), list));
        });
        handlers.put(MESSAGE_UPDATE, (event, data) -> {
            // How to model this?
        });
        
        // Audio
        handlers.put(AUDIO_TRACK_START, (event, data) -> {
            logger.debug("Got audio event {} with data {}", event.getType(), data);
            mewna.getPluginManager().processEvent(event.getType(), createAudioEvent(TrackMode.TRACK_START, data));
        });
        handlers.put(AUDIO_TRACK_STOP, (event, data) -> {
            logger.debug("Got audio event {} with data {}", event.getType(), data);
            mewna.getPluginManager().processEvent(event.getType(), createAudioEvent(TrackMode.TRACK_STOP, data));
        });
        handlers.put(AUDIO_TRACK_PAUSE, (event, data) -> {
            logger.debug("Got audio event {} with data {}", event.getType(), data);
            mewna.getPluginManager().processEvent(event.getType(), createAudioEvent(TrackMode.TRACK_PAUSE, data));
        });
        handlers.put(AUDIO_TRACK_QUEUE, (event, data) -> {
            logger.debug("Got audio event {} with data {}", event.getType(), data);
            mewna.getPluginManager().processEvent(event.getType(), createAudioEvent(TrackMode.TRACK_QUEUE, data));
        });
        handlers.put(AUDIO_TRACK_INVALID, (event, data) -> {
            logger.debug("Got audio event {} with data {}", event.getType(), data);
            mewna.getPluginManager().processEvent(event.getType(), createAudioEvent(TrackMode.TRACK_INVALID, data));
        });
        handlers.put(AUDIO_QUEUE_END, (event, data) -> {
            logger.debug("Got audio event {} with data {}", event.getType(), data);
            mewna.getPluginManager().processEvent(event.getType(), createAudioEvent(TrackMode.QUEUE_END, data));
        });
        handlers.put(AUDIO_TRACK_NOW_PLAYING, (event, data) -> {
            logger.debug("Got audio event {} with data {}", event.getType(), data);
            mewna.getPluginManager().processEvent(event.getType(), createAudioEvent(TrackMode.TRACK_NOW_PLAYING, data));
        });
        
        // Internal events
        handlers.put(LEVEL_UP, (event, data) -> {
            // Fetch user and guild
            final Guild guild = cache.getGuild(data.getString("guild"));
            final Channel channel = cache.getChannel(data.getString("channel"));
            final User user = cache.getUser(data.getString("user"));
            final long level = data.getLong("level");
            final long xp = data.getLong("xp");
            mewna.getPluginManager().processEvent(event.getType(), new LevelUpEvent(guild, channel, user, level, xp));
        });
        
        // Twitch events
        handlers.put(TWITCH_FOLLOWER, (event, data) -> {
            final String from = data.getJSONObject("from").toString();
            final String to = data.getJSONObject("to").toString();
            try {
                final TwitchStreamer fromStreamer = MAPPER.readValue(from, TwitchStreamer.class);
                final TwitchStreamer toStreamer = MAPPER.readValue(to, TwitchStreamer.class);
                mewna.getPluginManager().processEvent(event.getType(), new TwitchFollowerEvent(fromStreamer, toStreamer));
            } catch(final IOException e) {
                e.printStackTrace();
            }
        });
        handlers.put(TWITCH_STREAM_START, (event, data) -> {
            final String streamerDataString = data.getJSONObject("streamer").toString();
            final String streamDataString = data.getJSONObject("streamData").toString();
            try {
                final TwitchStreamer streamer = MAPPER.readValue(streamerDataString, TwitchStreamer.class);
                final TwitchStreamData streamData = MAPPER.readValue(streamDataString, TwitchStreamData.class);
                mewna.getPluginManager().processEvent(event.getType(), new TwitchStreamStartEvent(streamer, streamData));
            } catch(final IOException e) {
                e.printStackTrace();
            }
        });
        handlers.put(TWITCH_STREAM_END, (event, data) -> {
            try {
                final TwitchStreamer streamer = MAPPER.readValue(data.getJSONObject("streamer").toString(), TwitchStreamer.class);
                mewna.getPluginManager().processEvent(event.getType(), new TwitchStreamEndEvent(streamer));
            } catch(final IOException e) {
                e.printStackTrace();
            }
        });
        
        // We don't really care about these Discord gateway events
        handlers.put(GUILD_SYNC, (event, data) -> {
        });
        handlers.put(GUILD_BAN_ADD, (event, data) -> {
        });
        handlers.put(GUILD_BAN_REMOVE, (event, data) -> {
        });
        handlers.put(MESSAGE_REACTION_ADD, (event, data) -> {
        });
        handlers.put(MESSAGE_REACTION_REMOVE, (event, data) -> {
        });
        handlers.put(MESSAGE_REACTION_REMOVE_ALL, (event, data) -> {
        });
        handlers.put(PRESENCE_UPDATE, (event, data) -> {
        });
        handlers.put(READY, (event, data) -> {
        });
        handlers.put(TYPING_START, (event, data) -> {
        });
    }
    
    private AudioTrackEvent createAudioEvent(final TrackMode mode, final JSONObject data) {
        final JSONObject ctx = data.getJSONObject("ctx");
        final Guild guild = cache.getGuild(ctx.getString("guild"));
        final Channel channel = cache.getChannel(ctx.getString("channel"));
        final User user = cache.getUser(ctx.getString("userId"));
        if(data.has("info") && !data.isNull("info")) {
            final AudioTrackInfo info = AudioTrackInfo.fromJson(data.getJSONObject("info"));
            return new AudioTrackEvent(mode, guild, channel, user, info);
        } else {
            return new AudioTrackEvent(mode, guild, channel, user, null);
        }
    }
    
    public void handle(final SocketEvent event) {
        Optional.ofNullable(handlers.get(event.getType())).ifPresent(e -> e.accept(event, event.getData()));
    }
}
