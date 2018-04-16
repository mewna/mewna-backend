package gg.cute.event;

import gg.cute.Cute;
import gg.cute.cache.DiscordCache;
import gg.cute.cache.entity.Guild;
import gg.cute.nats.SocketEvent;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("unused")
public class EventHandler {
    @Getter
    private final Cute cute;
    
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Getter
    private final DiscordCache cache = new DiscordCache();
    
    public EventHandler(final Cute cute) {
        this.cute = cute;
    }
    
    public void handle(final SocketEvent event) {
        final JSONObject data = event.getData();
        switch(event.getType()) {
            // Channels
            case "CHANNEL_CREATE": {
                cache.cacheChannel(data);
                break;
            }
            case "CHANNEL_DELETE": {
                cache.deleteChannel(data.getString("id"));
                break;
            }
            case "CHANNEL_UPDATE": {
                cache.cacheChannel(data);
                break;
            }
            
            // Guilds
            case "GUILD_CREATE": {
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
                members.forEach(r -> {
                    cache.cacheUser(((JSONObject) r).getJSONObject("user"));
                    cache.cacheMember(id, (JSONObject) r);
                });
                break;
            }
            case "GUILD_DELETE": {
                if(data.has("unavailable")) {
                    logger.warn("Guild went unavailable: {}", data.getString("id"));
                } else {
                    cache.deleteGuild(data.getString("id"));
                }
                break;
            }
            case "GUILD_UPDATE": {
                cache.cacheGuild(data);
                break;
            }
            
            // Emotes
            // TODO: Do I REALLY care?
            case "GUILD_EMOJIS_UPDATE": {
                break;
            }
            
            // Members
            case "GUILD_MEMBER_ADD": {
                final JSONObject user = data.getJSONObject("user");
                cache.cacheUser(user);
                cache.cacheMember(data.getString("guild_id"), data);
                final Guild guild = cache.getGuild(data.getString("guild_id"));
                cache.getMappingManager().mapper(Guild.class).save(guild.toBuilder().memberCount(guild.getMemberCount() + 1).build());
                break;
            }
            case "GUILD_MEMBER_REMOVE": {
                final Guild guild = cache.getGuild(data.getString("guild_id"));
                cache.deleteMember(data.getString("guild_id"), data.getJSONObject("user").getString("id"));
                cache.getMappingManager().mapper(Guild.class).save(guild.toBuilder().memberCount(guild.getMemberCount() - 1).build());
                break;
            }
            case "GUILD_MEMBER_UPDATE": {
                // Rearrange the JSON to make it work
                final String guildId = data.getString("guild_id");
                cache.cacheMember(guildId, data);
                break;
            }
            case "GUILD_MEMBERS_CHUNK": {
                final JSONArray members = data.getJSONArray("members");
                members.forEach(m -> {
                    cache.cacheUser(((JSONObject) m).getJSONObject("user"));
                    cache.cacheMember(data.getString("guild_id"), (JSONObject) m);
                });
                break;
            }
            
            // Roles
            case "GUILD_ROLE_CREATE": {
                cache.cacheRole(data);
                break;
            }
            case "GUILD_ROLE_DELETE": {
                // data.role.id
                System.out.println("G_R_D: " + data);
                cache.deleteRole(data.getString("role_id"));
                break;
            }
            case "GUILD_ROLE_UPDATE": {
                cache.cacheRole(data);
                break;
            }
            
            // Users
            case "USER_UPDATE": {
                cache.cacheUser(data);
                break;
            }
            
            // Voice
            case "VOICE_SERVER_UPDATE": {
                // TODO: Handle voice...
                break;
            }
            
            // Messages
            case "MESSAGE_CREATE": {
                cute.getPluginManager().handleMessage(event.getData());
                break;
            }
            
            // We don't really care about these
            case "GUILD_SYNC":
            case "MESSAGE_DELETE":
            case "GUILD_BAN_ADD":
            case "GUILD_BAN_REMOVE":
            case "MESSAGE_DELETE_BULK":
            case "MESSAGE_REACTION_ADD":
            case "MESSAGE_REACTION_REMOVE":
            case "MESSAGE_REACTION_REMOVE_ALL":
            case "MESSAGE_UPDATE":
            case "PRESENCE_UPDATE":
            case "READY":
            case "TYPING_START":
            case "VOICE_STATE_UPDATE":
            default:
                break;
        }
    }
}
