package gg.cute.event;

import gg.cute.Cute;
import gg.cute.cache.DiscordCache;
import gg.cute.cache.entity.Guild;
import gg.cute.jda.RestJDA;
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
    
    private final RestJDA restJDA = new RestJDA(System.getenv("TOKEN"));
    
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
                cache.cacheGuild(data);
                final JSONArray roles = data.getJSONArray("roles");
                final JSONArray members = data.getJSONArray("members");
                final JSONArray channels = data.getJSONArray("channels");
                channels.forEach(r -> cache.cacheChannel((JSONObject) r));
                roles.forEach(r -> cache.cacheRole((JSONObject) r));
                members.forEach(r -> cache.cacheUser(((JSONObject) r).getJSONObject("user")));
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
                cache.cacheUser(data);
                final Guild guild = cache.getGuild(data.getString("guild_id"));
                cache.getMappingManager().mapper(Guild.class).save(guild.toBuilder().memberCount(guild.getMemberCount() + 1).build());
                break;
            }
            case "GUILD_MEMBER_REMOVE": {
                final Guild guild = cache.getGuild(data.getString("guild_id"));
                cache.getMappingManager().mapper(Guild.class).save(guild.toBuilder().memberCount(guild.getMemberCount() - 1).build());
                break;
            }
            case "GUILD_MEMBER_UPDATE": {
                // TODO: Do I care?
                break;
            }
            case "GUILD_MEMBERS_CHUNK": {
                final JSONArray members = data.getJSONArray("members");
                members.forEach(m -> cache.cacheUser(((JSONObject) m).getJSONObject("user")));
                break;
            }
            
            // Roles
            case "GUILD_ROLE_CREATE": {
                cache.cacheRole(data);
                break;
            }
            case "GUILD_ROLE_DELETE": {
                cache.deleteRole(data.getString("id"));
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
                if(data.getJSONObject("author").getString("id").equalsIgnoreCase("128316294742147072")) {
                    if(data.getString("content").equalsIgnoreCase("=help")) {
                        logger.info("Got MESSAGE_CREATE with data: {}", data);
                        restJDA.sendMessage(data.getString("channel_id"), "Magic!").queue();
                    }
                }
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
