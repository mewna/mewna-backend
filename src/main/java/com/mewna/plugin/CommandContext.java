package com.mewna.plugin;

import com.mewna.cache.entity.Channel;
import com.mewna.cache.entity.Guild;
import com.mewna.cache.entity.User;
import com.mewna.data.GuildSettings;
import com.mewna.data.Player;
import lombok.Value;

import java.util.List;

/**
 * @author amy
 * @since 4/8/18.
 */
@Value
public class CommandContext {
    private User user;
    private String command;
    private List<String> args;
    private String argstr;
    private Guild guild;
    private Channel channel;
    private List<User> mentions;
    private GuildSettings settings;
    private Player player;
    private long cost;
    private String prefix;
}
