package gg.cute.plugin;

import gg.cute.cache.entity.Channel;
import gg.cute.cache.entity.Guild;
import gg.cute.cache.entity.User;
import gg.cute.data.DiscordSettings;
import gg.cute.data.Player;
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
    private DiscordSettings settings;
    private Player player;
}
