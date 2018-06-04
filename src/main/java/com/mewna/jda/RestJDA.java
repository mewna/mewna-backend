package com.mewna.jda;

import com.mewna.Mewna;
import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.Member;
import com.mewna.cache.entity.Role;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.GuildImpl;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.entities.impl.SelfUserImpl;
import net.dv8tion.jda.core.entities.impl.TextChannelImpl;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route;
import net.dv8tion.jda.core.requests.Route.Channels;
import net.dv8tion.jda.core.requests.Route.CompiledRoute;
import net.dv8tion.jda.core.requests.Route.Guilds;
import net.dv8tion.jda.core.requests.Route.Messages;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import net.dv8tion.jda.core.utils.Checks;
import okhttp3.OkHttpClient;
import org.json.JSONObject;

import javax.annotation.CheckReturnValue;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Based off of Jagrosh's RestJDA for GiveawayBot.
 * <p/>
 * TODO: Figure out how to Redis-ratelimiter this
 *
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings({"unused", "TypeMayBeWeakened", "WeakerAccess"})
public class RestJDA {
    private final JDAImpl fakeJDA;
    
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    public RestJDA(final String token) {
        fakeJDA = new JDAImpl(AccountType.BOT, token, null, new OkHttpClient.Builder(), null,
                false, false, false, false,
                true, false, 2, 900, null);
        // Allow us to deal with upload size restrictions
        fakeJDA.setSelfUser(new SelfUserImpl(Long.parseLong(System.getenv("CLIENT_ID")), fakeJDA));
    }
    
    public RestAction<Void> sendTyping(final Channel channel) {
        final CompiledRoute route = Channels.SEND_TYPING.compile(channel.getId());
        return new BasicRestAction<>(fakeJDA, route);
    }
    
    @CheckReturnValue
    public RestAction<Void> addRoleToMember(final Guild guild, final Member member, final Role role) {
        Checks.notNull(role, "role");
        final CompiledRoute route = Guilds.ADD_MEMBER_ROLE.compile(guild.getId(), member.getId(), role.getId());
        return new BasicRestAction<>(fakeJDA, route);
    }
    
    @CheckReturnValue
    public RestAction<Void> removeRoleFromMember(final Guild guild, final Member member, final Role role) {
        Checks.notNull(role, "role");
        final CompiledRoute route = Guilds.REMOVE_MEMBER_ROLE.compile(guild.getId(), member.getId(), role.getId());
        
        return new BasicRestAction<>(fakeJDA, route);
    }
    
    @CheckReturnValue
    public RestAction<Void> addManyRolesToMember(final Guild guild, final Member member, final String... roles) {
        Checks.notNull(guild, "guild");
        Checks.notNull(member, "member");
        Checks.notNull(roles, "null roles");
        Checks.notEmpty(roles, "empty roles");
        
        final Set<String> finalRoles = Arrays.stream(roles).filter(e -> {
            final Role role = Mewna.getInstance().getCache().getRole(e);
            // Don't add @everyone or managed roles
            return !role.getId().equalsIgnoreCase(guild.getId()) && !role.isManaged();
        }).collect(Collectors.toSet());
        if(member.getRoles() != null && !member.getRoles().isEmpty()) {
            finalRoles.addAll(member.getRoles());
        }
        final CompiledRoute route = Guilds.MODIFY_MEMBER.compile(guild.getId(), member.getId());
        final JSONObject body = new JSONObject().put("roles", finalRoles);
        return new BasicRestAction<>(fakeJDA, route, body);
    }
    
    @CheckReturnValue
    public RestAction<Void> removeManyRolesFromMember(final Guild guild, final Member member, final String... roles) {
        Checks.notNull(guild, "guild");
        Checks.notNull(member, "member");
        Checks.notNull(roles, "null roles");
        Checks.notEmpty(roles, "empty roles");
        
        final Set<String> rolesToRemove = Arrays.stream(roles).filter(e -> {
            final Role role = Mewna.getInstance().getCache().getRole(e);
            // Don't remove @everyone or managed roles
            return !role.getId().equalsIgnoreCase(guild.getId()) && !role.isManaged();
        }).collect(Collectors.toSet());
        final Set<String> memberRoles = new HashSet<>(member.getRoles());
        memberRoles.removeAll(rolesToRemove);
        final CompiledRoute route = Guilds.MODIFY_MEMBER.compile(guild.getId(), member.getId());
        final JSONObject body = new JSONObject().put("roles", memberRoles);
        return new BasicRestAction<>(fakeJDA, route, body);
    }
    
    public RestAction<Void> addAndRemoveManyRolesForMember(final Guild guild, final Member member, List<String> rolesToAdd,
                                                           List<String> rolesToRemove) {
        Checks.notNull(guild, "guild");
        Checks.notNull(member, "member");
        Checks.notNull(rolesToAdd, "null add roles");
        Checks.notNull(rolesToRemove, "null remove roles");
        
        rolesToAdd = rolesToAdd.stream().filter(e -> {
            final Role role = Mewna.getInstance().getCache().getRole(e);
            // Don't add @everyone or managed roles
            return !role.getId().equalsIgnoreCase(guild.getId()) && !role.isManaged();
        }).collect(Collectors.toList());
        rolesToRemove = rolesToRemove.stream().filter(e -> {
            final Role role = Mewna.getInstance().getCache().getRole(e);
            // Don't remove @everyone or managed roles
            return !role.getId().equalsIgnoreCase(guild.getId()) && !role.isManaged();
        }).collect(Collectors.toList());
        
        final Set<String> finalRoles = new HashSet<>();
        final List<String> memberRoles = member.getRoles();
        if(memberRoles != null) {
            finalRoles.addAll(memberRoles);
        }
        finalRoles.addAll(rolesToAdd);
        finalRoles.removeAll(rolesToRemove);
        final CompiledRoute route = Guilds.MODIFY_MEMBER.compile(guild.getId(), member.getId());
        final JSONObject body = new JSONObject().put("roles", finalRoles);
        return new BasicRestAction<>(fakeJDA, route, body);
    }
    
    @CheckReturnValue
    public MessageAction editMessage(final long channelId, final long messageId, final Message newContent) {
        Checks.notNull(newContent, "message");
        final CompiledRoute route = Messages.EDIT_MESSAGE.compile(Long.toString(channelId), Long.toString(messageId));
        return new MessageAction(fakeJDA, route, new TextChannelImpl(channelId, new GuildImpl(fakeJDA, 0))).apply(newContent);
    }
    
    @CheckReturnValue
    public MessageAction sendFile(final Channel channel, final byte[] file, final String fileName, final Message message) {
        Checks.notNull(file, "data File");
        Checks.notNull(fileName, "fileName");
        final String channelId = channel.getId();
        
        final CompiledRoute route = Messages.SEND_MESSAGE.compile(channelId);
        return new MessageAction(fakeJDA, route, new TextChannelImpl(Long.parseLong(channelId), new GuildImpl(fakeJDA, 0)))
                .apply(message).addFile(file, fileName);
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(final Channel channel, final String msg) {
        return sendMessage(channel.getId(), new MessageBuilder().append(msg).build());
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(final long channelId, final String msg) {
        return sendMessage(channelId, new MessageBuilder().append(msg).build());
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(final Channel channel, final MessageEmbed embed) {
        return sendMessage(channel.getId(), new MessageBuilder().setEmbed(embed).build());
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(final String channelId, final MessageEmbed embed) {
        return sendMessage(channelId, new MessageBuilder().setEmbed(embed).build());
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(final long channelId, final MessageEmbed embed) {
        return sendMessage(channelId, new MessageBuilder().setEmbed(embed).build());
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(final String channelId, final String msg) {
        return sendMessage(channelId, new MessageBuilder().append(msg).build());
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(final long channelId, final Message msg) {
        return sendMessage(Long.toString(channelId), msg);
    }
    
    @CheckReturnValue
    public MessageAction sendMessage(final String channelId, final Message msg) {
        Checks.notNull(msg, "Message");
        final CompiledRoute route = Messages.SEND_MESSAGE.compile(channelId);
        return new MessageAction(fakeJDA, route, new TextChannelImpl(Long.parseLong(channelId),
                new GuildImpl(fakeJDA, 0))).apply(msg);
    }
    
    @CheckReturnValue
    public RestAction<MessageJson> getMessageById(final long channelId, final long messageId) {
        final CompiledRoute route = Messages.GET_MESSAGE.compile(Long.toString(channelId), Long.toString(messageId));
        return new RestAction<MessageJson>(fakeJDA, route) {
            @Override
            protected void handleResponse(final Response response, final Request<MessageJson> request) {
                if(response.isOk()) {
                    request.onSuccess(new MessageJson(response.getObject()));
                } else {
                    request.onFailure(response);
                }
            }
        };
    }
    
    private final class BasicRestAction<T> extends RestAction<T> {
        private BasicRestAction(final JDA api, final CompiledRoute route) {
            super(api, route);
        }
        
        private BasicRestAction(final JDA api, final CompiledRoute route, final JSONObject body) {
            super(api, route, body);
        }
        
        @Override
        protected void handleResponse(final Response response, final Request<T> request) {
            if(response.isOk()) {
                request.onSuccess(null);
            } else {
                request.onFailure(response);
            }
        }
    }
}