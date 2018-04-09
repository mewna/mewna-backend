package gg.cute.cache;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import gg.cute.cache.entity.Channel;
import gg.cute.cache.entity.Channel.ChannelBuilder;
import gg.cute.cache.entity.Guild;
import gg.cute.cache.entity.Guild.GuildBuilder;
import gg.cute.cache.entity.Role;
import gg.cute.cache.entity.Role.RoleBuilder;
import gg.cute.cache.entity.User;
import gg.cute.cache.entity.User.UserBuilder;
import lombok.Getter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("unused")
public class DiscordCache {
    @Getter
    private final Cluster cluster;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Getter
    private Session session;
    @Getter
    private MappingManager mappingManager;
    
    public DiscordCache() {
        cluster = Cluster.builder().addContactPoint(System.getenv("CASSANDRA_HOST")).build();
    }
    
    public void connect() {
        session = cluster.connect();
        mappingManager = new MappingManager(session);
        // Make keyspace
        session.execute("CREATE KEYSPACE IF NOT EXISTS cute WITH REPLICATION = {'class' : 'SimpleStrategy', 'replication_factor' : 1}");
        // Guilds
        session.execute("CREATE TABLE IF NOT EXISTS cute.guilds (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT," +
                "icon TEXT," +
                "ownerId TEXT," +
                "region TEXT," +
                "memberCount INT" +
                ");");
        //session.execute("CREATE INDEX IF NOT EXISTS guild_names ON cute.guilds (name);");
        // Channels
        session.execute("CREATE TABLE IF NOT EXISTS cute.channels (" +
                "id TEXT PRIMARY KEY," +
                "type INT," +
                "guildId TEXT," +
                "name TEXT," +
                "nsfw BOOLEAN" +
                ");");
        //session.execute("CREATE INDEX IF NOT EXISTS channel_names ON cute.channels (name);");
        // Roles
        session.execute("CREATE TABLE IF NOT EXISTS cute.roles (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT," +
                "color INT" +
                ");");
        //session.execute("CREATE INDEX IF NOT EXISTS role_names ON cute.roles (name);");
        // Users
        session.execute("CREATE TABLE IF NOT EXISTS cute.users (" +
                "id TEXT PRIMARY KEY," +
                "name TEXT," +
                "discriminator TEXT," +
                "bot BOOLEAN," +
                "avatar TEXT," +
                ");");
        //session.execute("CREATE INDEX IF NOT EXISTS user_names ON cute.users (name);");
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
        logger.info("Updated guild {}", id);
    }
    
    public Guild getGuild(final String id) {
        return mappingManager.mapper(Guild.class).get(id);
    }
    
    public void deleteGuild(final String id) {
        mappingManager.mapper(Guild.class).delete(id);
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
        logger.info("Updated channel {}", id);
    }
    
    public Channel getChannel(final String id) {
        return mappingManager.mapper(Channel.class).get(id);
    }
    
    public void deleteChannel(final String id) {
        mappingManager.mapper(Channel.class).delete(id);
    }
    
    public void cacheUser(final JSONObject data) {
        final String id = data.getString("id");
        final User old = mappingManager.mapper(User.class).get(id);
        final UserBuilder user = User.builder().id(id);
        if(data.has("name")) {
            user.name(data.getString("name"));
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
        logger.info("Updated user {}", id);
    }
    
    public User getUser(final String id) {
        return mappingManager.mapper(User.class).get(id);
    }
    
    public void cacheRole(final JSONObject data) {
        final String id = data.getString("id");
        final Role old = mappingManager.mapper(Role.class).get(id);
        final RoleBuilder role = Role.builder().id(id);
        if(data.has("name")) {
            role.name(data.getString("name"));
        } else if(old != null) {
            role.name(old.getName());
        }
        if(data.has("color")) {
            role.color(data.getInt("color"));
        } else if(old != null) {
            role.color(old.getColor());
        }
        mappingManager.mapper(Role.class).save(role.build());
        logger.info("Updated role {}", id);
    }
    
    public Role getRole(final String id) {
        return mappingManager.mapper(Role.class).get(id);
    }
    
    public void deleteRole(final String id) {
        mappingManager.mapper(Role.class).delete(id);
    }
}
