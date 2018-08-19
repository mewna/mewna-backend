package com.mewna.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.Mewna;
import com.mewna.cache.entity.*;
import io.sentry.Sentry;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

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
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @SuppressWarnings("FieldCanBeLocal")
    private final Mewna mewna;
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private final ObjectMapper mapper = new ObjectMapper();
    
    public DiscordCache(final Mewna mewna) {
        this.mewna = mewna;
    }
    
    public void connect() {
    }
    
    @SuppressWarnings({"ConstantConditions", "UnnecessarilyQualifiedInnerClassAccess"})
    private String get(final String url) {
        try {
            return client.newCall(new Request.Builder().get().url(System.getenv("SHARDS_URL") + url).build())
                    .execute().body().string();
        } catch(final NullPointerException | IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    public Guild getGuild(final String id) {
        try {
            return mapper.readValue(get("/cache/guild/" + id), Guild.class);
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    public Member getMember(final Guild guild, final User user) {
        try {
            return mapper.readValue(get("/cache/guild/" + guild.getId() + "/member/" + user.getId()), Member.class);
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    public Channel getChannel(final String channel) {
        try {
            return mapper.readValue(get("/cache/channel/" + channel), Channel.class);
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    public List<Channel> getGuildChannels(final String id) {
        try {
            return mapper.readValue(get("/cache/guild/" + id + "/channels"), new TypeReference<List<Channel>>(){});
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    public User getUser(final String id) {
        try {
            return mapper.readValue(get("/cache/user/" + id), User.class);
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    public Role getRole(final String id) {
        try {
            return mapper.readValue(get("/cache/roles/" + id), Role.class);
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
    
    public List<Role> getGuildRoles(final String id) {
        try {
            return mapper.readValue(get("/cache/guild/" + id + "/roles"), new TypeReference<List<Role>>(){});
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new IllegalStateException(e);
        }
    }
}
