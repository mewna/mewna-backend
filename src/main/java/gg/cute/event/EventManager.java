package gg.cute.event;

import gg.cute.Cute;
import gg.cute.cache.DiscordCache;
import gg.cute.cache.entity.Guild;
import gg.cute.nats.SocketEvent;
import gg.cute.plugin.event.guild.member.GuildMemberAddEvent;
import gg.cute.plugin.event.guild.member.GuildMemberRemoveEvent;
import gg.cute.plugin.event.message.MessageDeleteBulkEvent;
import gg.cute.plugin.event.message.MessageDeleteEvent;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static gg.cute.plugin.event.EventType.*;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("unused")
public class EventManager {
    @Getter
    private final Cute cute;
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Getter
    private final DiscordCache cache = new DiscordCache();
    
    private final Map<String, BiConsumer<SocketEvent, JSONObject>> handlers = new HashMap<>();
    
    public EventManager(final Cute cute) {
        this.cute = cute;
        
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
            cute.getPluginManager().processEvent(event.getType(),
                    new GuildMemberAddEvent(guild, cache.getMember(guild, cache.getUser(user.getString("id")))));
        });
        handlers.put(GUILD_MEMBER_REMOVE, (event, data) -> {
            final Guild guild = cache.getGuild(data.getString("guild_id"));
            cache.deleteMember(data.getString("guild_id"), data.getJSONObject("user").getString("id"));
            cache.getMappingManager().mapper(Guild.class).save(guild.toBuilder().memberCount(guild.getMemberCount() - 1).build());
            cute.getPluginManager().processEvent(event.getType(), new GuildMemberRemoveEvent(guild,
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
        handlers.put(GUILD_ROLE_DELETE, (event, data) -> {
            // data.role.id
            System.out.println("G_R_D: " + data);
            cache.deleteRole(data.getString("role_id"));
        });
        handlers.put(GUILD_ROLE_UPDATE, (event, data) -> cache.cacheRole(data));
    
        // Users
        handlers.put(USER_UPDATE, (event, data) -> cache.cacheUser(data));
    
        // Voice
        handlers.put(VOICE_SERVER_UPDATE, (event, data) -> {
            // TODO: Handle voice...
        });
    
        // Messages
        handlers.put(MESSAGE_CREATE, (event, data) -> {
            // This will pass down to the event handler, so we don't need to worry
            // TODO: Handling webhooks
            cute.getPluginManager().tryExecCommand(event.getData());
        });
        handlers.put(MESSAGE_DELETE, (event, data) -> {
            // TODO: Would have to cache messages...
            cute.getPluginManager().processEvent(event.getType(), new MessageDeleteEvent(data.getString("id"),
                    cache.getChannel(data.getString("channel_id"))));
        });
        handlers.put(MESSAGE_DELETE_BULK, (event, data) -> {
            // TODO: Would have to cache messages...
            final JSONArray jsonArray = data.getJSONArray("ids");
            final List<String> list = new ArrayList<>();
            jsonArray.forEach(e -> list.add((String) e));
            cute.getPluginManager().processEvent(event.getType(),
                    new MessageDeleteBulkEvent(cache.getChannel(data.getString("channel_id")), list));
        });
        handlers.put(MESSAGE_UPDATE, (event, data) -> {
            // TODO: How to model this?
        });
    
        // We don't really care about these
        handlers.put(GUILD_SYNC, (event, data) -> {});
        handlers.put(GUILD_BAN_ADD, (event, data) -> {});
        handlers.put(GUILD_BAN_REMOVE, (event, data) -> {});
        handlers.put(MESSAGE_REACTION_ADD, (event, data) -> {});
        handlers.put(MESSAGE_REACTION_REMOVE, (event, data) -> {});
        handlers.put(MESSAGE_REACTION_REMOVE_ALL, (event, data) -> {});
        handlers.put(PRESENCE_UPDATE, (event, data) -> {});
        handlers.put(READY, (event, data) -> {});
        handlers.put(TYPING_START, (event, data) -> {});
        handlers.put(VOICE_STATE_UPDATE, (event, data) -> {});
        
    }
    
    public void handle(final SocketEvent event) {
        handlers.get(event.getType()).accept(event, event.getData());
    }
}
