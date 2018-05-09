package com.mewna.cache;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.mewna.Mewna;
import com.mewna.cache.entity.*;
import com.mewna.cache.entity.Channel.ChannelBuilder;
import com.mewna.cache.entity.Guild.GuildBuilder;
import com.mewna.cache.entity.Member.MemberBuilder;
import com.mewna.cache.entity.Role.RoleBuilder;
import com.mewna.cache.entity.User.UserBuilder;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO: Move this to a multi-level Cassandra / Redis cache
 * TODO: Consider storing users in ElasticSearch?
 *
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("unused")
public class DiscordCache {
    private static final String SELF_VOICE_STATES = "self-voice-states";
    private static final String USER_VOICE_STATES = "user-voice-states";
    @Getter
    private final Cluster cluster;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Mewna mewna;
    @Getter
    private Session session;
    @Getter
    private MappingManager mappingManager;
    
    public DiscordCache(final Mewna mewna) {
        this.mewna = mewna;
        cluster = Cluster.builder().addContactPoint(System.getenv("CASSANDRA_HOST")).build();
    }
    
    public void connect() {
        session = cluster.connect();
        mappingManager = new MappingManager(session);
        // Make keyspace
        session.execute("CREATE KEYSPACE IF NOT EXISTS mewna WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1}");
        // Guilds
        session.execute("CREATE TABLE IF NOT EXISTS mewna.guilds (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT," +
                "icon TEXT," +
                "ownerId TEXT," +
                "region TEXT," +
                "memberCount INT" +
                ");");
        session.execute("CREATE INDEX IF NOT EXISTS ON mewna.guilds (ownerId);");
        // Channels
        session.execute("CREATE TABLE IF NOT EXISTS mewna.channels (" +
                "id TEXT PRIMARY KEY," +
                "type INT," +
                "guildId TEXT," +
                "name TEXT," +
                "nsfw BOOLEAN" +
                ");");
        session.execute("CREATE INDEX IF NOT EXISTS ON mewna.channels (guildId);");
        // Roles
        session.execute("CREATE TABLE IF NOT EXISTS mewna.roles (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT," +
                "color INT," +
                "guildId TEXT" +
                ");");
        session.execute("CREATE INDEX IF NOT EXISTS ON mewna.roles (guildId);");
        // Users
        session.execute("CREATE TABLE IF NOT EXISTS mewna.users (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT," +
                "discriminator TEXT," +
                "bot BOOLEAN," +
                "avatar TEXT," +
                ");");
        session.execute("CREATE INDEX IF NOT EXISTS ON mewna.users (name);");
        // Overwrites
        session.execute("CREATE TABLE IF NOT EXISTS mewna.overwrites (" +
                "channel TEXT PRIMARY KEY," +
                "id TEXT," +
                "type TEXT," +
                // Note that "allow" is a reserved keyword, so we wrap it in quotes to bypass that
                "\"allow\" BIGINT," +
                "deny BIGINT," +
                ");");
        session.execute("CREATE INDEX IF NOT EXISTS ON mewna.overwrites (id);");
        // Members
        session.execute("CREATE TABLE IF NOT EXISTS mewna.members (" +
                "guildId TEXT," +
                "id TEXT," +
                "nick TEXT," +
                "joinedAt TEXT," +
                "roles LIST<TEXT>," +
                "PRIMARY KEY (guildId, id)," +
                ");");
    }
    
    public void shutdown() {
        if(session != null && !session.isClosed()) {
            session.close();
        }
        if(cluster != null && !cluster.isClosed()) {
            cluster.close();
        }
    }
    
    public void cacheGuild(final JSONObject data) {
        final String id = data.getString("id");
        final Guild old = mappingManager.mapper(Guild.class).get(id);
        // Interleave as needed
        final GuildBuilder guild = Guild.builder().id(id);
        // name
        if(data.has("name")) {
            guild.name(data.getString("name"));
        } else if(old != null) {
            guild.name(old.getName());
        }
        // icon
        if(data.has("icon")) {
            if(!data.isNull("icon")) {
                guild.icon(data.getString("icon"));
            }
        } else if(old != null) {
            guild.icon(old.getIcon());
        }
        // owner
        if(data.has("owner_id")) {
            guild.ownerId(data.getString("owner_id"));
        } else if(old != null) {
            guild.ownerId(old.getOwnerId());
        }
        // region
        if(data.has("region")) {
            guild.region(data.getString("region"));
        } else if(old != null) {
            guild.region(old.getRegion());
        }
        // member count
        if(data.has("member_count")) {
            guild.memberCount(data.getInt("member_count"));
        } else if(old != null) {
            guild.memberCount(old.getMemberCount());
        }

        mappingManager.mapper(Guild.class).save(guild.build());
        logger.debug("Updated guild {}", id);
    }
    
    public Guild getGuild(final String id) {
        return mappingManager.mapper(Guild.class).get(id);
    }
    
    public void deleteGuild(final String id) {
        mappingManager.mapper(Guild.class).delete(id);
    }
    
    public void cacheMember(final String guildId, final JSONObject json) {
        final String id = json.getJSONObject("user").getString("id");
        final Member old = mappingManager.mapper(Member.class).get(guildId, id);
        final MemberBuilder member = Member.builder();
        member.id(id).guildId(guildId);
        if(json.has("nick")) {
            if(!json.isNull("nick")) {
                member.nick(json.getString("nick"));
            }
        } else if(old != null && old.getNick() != null) {
            member.nick(old.getNick());
        }
        final List<String> roles = new ArrayList<>();
        json.getJSONArray("roles").forEach(r -> roles.add((String) r));
        final Set<String> mergedRoles = new HashSet<>(roles);
        if(old != null && old.getRoles() != null) {
            mergedRoles.addAll(old.getRoles());
        }
        member.roles(new ArrayList<>(mergedRoles));
        if(json.has("joined_at")) {
            member.joinedAt(json.getString("joined_at"));
        } else if(old != null && old.getJoinedAt() != null) {
            member.joinedAt(old.getJoinedAt());
        }
        mappingManager.mapper(Member.class).save(member.build());
    }
    
    public Member getMember(final Guild guild, final User user) {
        return mappingManager.mapper(Member.class).get(guild.getId(), user.getId());
    }
    
    public void deleteMember(final String guildId, final String userId) {
        mappingManager.mapper(Member.class).delete(guildId, userId);
    }
    
    public List<PermissionOverwrite> getOverwrites(final Channel channel) {
        // Grab all
        // TODO: Use an accessor here?
        final ResultSet res = session.execute(String.format("SELECT * FROM mewna.overwrites WHERE channel = %s;", channel.getId()));
        return mappingManager.mapper(PermissionOverwrite.class).map(res).all();
    }
    
    public void cacheChannel(final JSONObject data) {
        final String id = data.getString("id");
        final Channel old = mappingManager.mapper(Channel.class).get(id);
        // Interleave as needed
        final ChannelBuilder channel = Channel.builder().id(id);
        // nsfw
        if(data.has("nsfw")) {
            channel.nsfw(data.getBoolean("nsfw"));
        } else if(old != null) {
            channel.nsfw(old.isNsfw());
        }
        // name
        if(data.has("name")) {
            channel.name(data.getString("name"));
        } else if(old != null) {
            channel.name(old.getName());
        }
        // guild id
        if(data.has("guild_id")) {
            channel.guildId(data.getString("guild_id"));
        } else if(old != null) {
            channel.guildId(old.getGuildId());
        }
        // type
        if(data.has("type")) {
            channel.type(data.getInt("type"));
        } else if(old != null) {
            channel.type(old.getType());
        }
        mappingManager.mapper(Channel.class).save(channel.build());
        
        // Permission overwrites
        if(data.has("permission_overwrites")) {
            if(!data.isNull("permission_overwrites")) {
                // Delete old overwrites just to be safe
                session.execute("DELETE FROM mewna.overwrites WHERE channel = '" + id + "'; ");
                final JSONArray overs = data.getJSONArray("permission_overwrites");
                for(final Object o : overs) {
                    final JSONObject over = (JSONObject) o;
                    final PermissionOverwrite permissionOverwrite = new PermissionOverwrite(id, over.getString("id"),
                            over.getString("type"), over.getLong("allow"), over.getLong("deny"));
                    mappingManager.mapper(PermissionOverwrite.class).save(permissionOverwrite);
                }
            }
        }
        logger.debug("Updated channel {}", id);
    }
    
    public Channel getChannel(final String id) {
        return mappingManager.mapper(Channel.class).get(id);
    }
    
    public List<Channel> getGuildChannels(final String id) {
        final ResultSet rs = session.execute("SELECT * FROM mewna.channels WHERE guildId = '" + id + "';");
        final List<Channel> results = new ArrayList<>(mappingManager.mapper(Channel.class).map(rs).all());
        return results;
    }
    
    public void deleteChannel(final String id) {
        mappingManager.mapper(Channel.class).delete(id);
    }
    
    // TODO: Consider moving these to redis instead
    public void cacheUser(final JSONObject data) {
        final String id = data.getString("id");
        final User old = mappingManager.mapper(User.class).get(id);
        final UserBuilder user = User.builder().id(id);
        if(data.has("username")) {
            user.name(data.getString("username"));
        } else if(old != null) {
            user.name(old.getName());
        }
        if(data.has("discriminator")) {
            user.discriminator(data.getString("discriminator"));
        } else if(old != null) {
            user.discriminator(old.getDiscriminator());
        }
        if(data.has("bot")) {
            user.bot(data.getBoolean("bot"));
        } else if(old != null) {
            user.bot(old.isBot());
        } else {
            user.bot(false);
        }
        if(data.has("avatar")) {
            if(!data.isNull("avatar")) {
                user.avatar(data.getString("avatar"));
            }
        }
        mappingManager.mapper(User.class).save(user.build());
        logger.debug("Updated user {}", id);
    }
    
    public User getUser(final String id) {
        return mappingManager.mapper(User.class).get(id);
    }
    
    public void cacheRole(final JSONObject data) {
        final JSONObject rData = data.getJSONObject("role");
        final String id = rData.getString("id");
        final Role old = mappingManager.mapper(Role.class).get(id);
        final RoleBuilder role = Role.builder().id(id);
        if(rData.has("name")) {
            role.name(rData.getString("name"));
        } else if(old != null) {
            role.name(old.getName());
        }
        if(rData.has("color")) {
            role.color(rData.getInt("color"));
        } else if(old != null) {
            role.color(old.getColor());
        }
        if(data.has("guild_id")) {
            role.guildId(data.getString("guild_id"));
        } else if(old != null) {
            role.guildId(old.getGuildId());
        }
        mappingManager.mapper(Role.class).save(role.build());
        logger.debug("Updated role {}", id);
    }
    
    public Role getRole(final String id) {
        return mappingManager.mapper(Role.class).get(id);
    }
    
    public List<Role> getGuildRoles(final String id) {
        final ResultSet rs = session.execute("SELECT * FROM mewna.roles WHERE guildId = '" + id + "';");
        final List<Role> results = new ArrayList<>(mappingManager.mapper(Role.class).map(rs).all());
        return results;
    }
    
    public void deleteRole(final String id) {
        mappingManager.mapper(Role.class).delete(id);
    }
    
    public void cacheVoiceState(final JSONObject data) {
        final String userId = data.getString("user_id");
        final String channelId = data.has("channel_id") && !data.isNull("channel_id") ? data.getString("channel_id") : null;
        final String guildId;
        if(data.has("guild_id") && !data.isNull("guild_id")) {
            guildId = data.getString("guild_id");
        } else {
            guildId = channelId != null ? getChannel(channelId).getGuildId() : null;
        }
        if(userId.equalsIgnoreCase(System.getenv("CLIENT_ID"))) {
            // Cache self voice state
            if(guildId != null) {
                mewna.getDatabase().redis(c -> c.hset(SELF_VOICE_STATES, guildId, data.toString()));
            }
        } else {
            // Cache user voice state
            // TODO: What about bots?
            mewna.getDatabase().redis(c -> c.hset(USER_VOICE_STATES, userId, data.toString()));
        }
    }
    
    public VoiceState getSelfVoiceState(final String guildId) {
        final VoiceState[] state = {null};
        mewna.getDatabase().redis(c -> {
            try {
                final String res = c.hget(SELF_VOICE_STATES, guildId);
                final JSONObject data = new JSONObject(res);
                state[0] = new VoiceState(getGuild(data.getString("guild_id")),
                        getChannel(data.getString("channel_id")),
                        getUser(data.getString("user_id")));
            } catch(final JSONException | NullPointerException e) {
                state[0] = null;
            }
        });
        return state[0];
    }
    
    public VoiceState getVoiceState(final String userId) {
        final VoiceState[] state = {null};
        mewna.getDatabase().redis(c -> {
            try {
                final String res = c.hget(USER_VOICE_STATES, userId);
                final JSONObject data = new JSONObject(res);
                state[0] = new VoiceState(getGuild(data.getString("guild_id")),
                        getChannel(data.getString("channel_id")),
                        getUser(data.getString("user_id")));
            } catch(final JSONException | NullPointerException e) {
                state[0] = null;
            }
        });
        return state[0];
    }
    
    public void deleteSelfVoiceState(final String guildId) {
        mewna.getDatabase().redis(c -> c.hdel(SELF_VOICE_STATES, guildId));
    }
}
