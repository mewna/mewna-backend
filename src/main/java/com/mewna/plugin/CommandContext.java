package com.mewna.plugin;

import com.mewna.accounts.Account;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.user.User;
import com.mewna.data.Player;
import com.mewna.util.Profiler;
import lombok.Builder;
import lombok.Value;

import java.util.List;

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
}
