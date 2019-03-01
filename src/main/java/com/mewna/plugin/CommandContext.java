package com.mewna.plugin;

import com.mewna.accounts.Account;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.message.Embed;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.user.User;
import com.mewna.data.Player;
import com.mewna.event.discord.DiscordMessageCreate;
import com.mewna.util.Profiler;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author amy
 * @since 4/8/18.
 */
@Value
@Builder(toBuilder = true)
public class CommandContext {
    private User user;
    private String command;
    private List<String> args;
    private String argstr;
    private Guild guild;
    private Message message;
    private List<User> mentions;
    private Player player;
    private Account account;
    private long cost;
    private String prefix;
    private String language;
    private String currencySymbol;
    private Profiler profiler;
    private DiscordMessageCreate source;
    
    public CompletionStage<Message> sendMessage(final String msg) {
        return message.catnip().rest().channel().sendMessage(message.channelId(), msg);
    }
    
    public CompletionStage<Message> sendMessage(final Embed embed) {
        return message.catnip().rest().channel().sendMessage(message.channelId(), embed);
    }
    
    public CompletionStage<Message> sendMessage(final MessageOptions options) {
        return message.catnip().rest().channel().sendMessage(message.channelId(), options);
    }
    
    public CompletionStage<Message> sendMessage(final Message msg) {
        return message.catnip().rest().channel().sendMessage(message.channelId(), msg);
    }
}
