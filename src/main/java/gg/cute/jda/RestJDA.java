package gg.cute.jda;

import gg.cute.cache.entity.Channel;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.GuildImpl;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.entities.impl.TextChannelImpl;
import net.dv8tion.jda.core.requests.Request;
import net.dv8tion.jda.core.requests.Response;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.Route.CompiledRoute;
import net.dv8tion.jda.core.requests.Route.Messages;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import net.dv8tion.jda.core.utils.Checks;
import okhttp3.OkHttpClient;

import javax.annotation.CheckReturnValue;

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
    }
    
    @CheckReturnValue
    public MessageAction editMessage(final long channelId, final long messageId, final Message newContent) {
        Checks.notNull(newContent, "message");
        final CompiledRoute route = Messages.EDIT_MESSAGE.compile(Long.toString(channelId), Long.toString(messageId));
        return new MessageAction(fakeJDA, route, new TextChannelImpl(channelId, new GuildImpl(fakeJDA, 0))).apply(newContent);
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
}