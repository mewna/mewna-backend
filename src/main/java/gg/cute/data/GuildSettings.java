package gg.cute.data;

import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import gg.cute.data.config.Config;
import lombok.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static gg.cute.data.config.Constraints.*;

/**
 * @author amy
 * @since 4/10/18.
 */
@Value
@Table("discord_settings")
@Index("id")
public class GuildSettings {
    @PrimaryKey
    private String id;
    
    // Command settings
    
    @Config(name = "customPrefix", constraints = {TYPE_STRING, STRING_LEN_8})
    private String customPrefix;
    @Config(name = "commandSettings", constraints = {TYPE_MAP, VALUE_NOT_NULL})
    private Map<String, CommandSettings> commandSettings;
    
    // Twitch notification settings
    
    @Config(name = "twitchWebhookChannel", constraints = {TYPE_STRING, VALUE_STRING_NOT_EMPTY})
    private String twitchWebhookChannel;
    @Config(name = "twitchStreamers", constraints = {TYPE_LIST, VALUE_NOT_NULL})
    private Set<String> twitchStreamers;
    
    // Economy settings
    
    @Config(name = "currencySymbol", constraints = {TYPE_STRING, VALUE_NOT_NULL, STRING_LEN_32})
    private String currencySymbol;
    
    // Levels settings
    
    @Config(name = "levelsEnabled", constraints = {TYPE_BOOLEAN, VALUE_NOT_NULL})
    private boolean levelsEnabled;
    @Config(name = "levelUpMessagesEnabled", constraints = {TYPE_BOOLEAN, VALUE_NOT_NULL})
    private boolean levelUpMessagesEnabled;
    @Config(name = "levelUpCards", constraints = {TYPE_BOOLEAN, VALUE_NOT_NULL})
    private boolean levelUpCards;
    @Config(name = "levelUpMessage", constraints = {TYPE_STRING, VALUE_NOT_NULL, STRING_LEN_2K})
    private String levelUpMessage;
    @Config(name = "levelRoleRewards", constraints = {TYPE_MAP, VALUE_NOT_NULL})
    private Map<Integer, Set<String>> levelRoleRewards;
    
    // Welcoming settings
    
    @Config(name = "welcomeChannel", constraints = {TYPE_STRING, VALUE_NOT_NULL})
    private String welcomeChannel;
    @Config(name = "joinRoleId", constraints = {TYPE_STRING, VALUE_NOT_NULL})
    private String joinRoleId;
    @Config(name = "enableWelcomeMessages", constraints = {TYPE_BOOLEAN, VALUE_NOT_NULL})
    private boolean enableWelcomeMessages;
    @Config(name = "welcomeMessage", constraints = {TYPE_STRING, VALUE_NOT_NULL, VALUE_STRING_NOT_EMPTY, STRING_LEN_2K})
    private String welcomeMessage;
    @Config(name = "enableGoodbyeMessages", constraints = {TYPE_BOOLEAN, VALUE_NOT_NULL})
    private boolean enableGoodbyeMessages;
    @Config(name = "goodbyeMessage", constraints = {TYPE_STRING, VALUE_NOT_NULL, VALUE_STRING_NOT_EMPTY, STRING_LEN_2K})
    private String goodbyeMessage;
    
    public static GuildSettings base(final String id) {
        return new GuildSettings(id,
                // Commands settings
                null, new HashMap<>(),
                // Twitch settings
                null, new HashSet<>(),
                // Economy settings
                null,
                // Levels settings
                false, true, true,
                "{user.name} leveled :up: to {level}! :tada:", new HashMap<>(),
                // Welcoming settings
                null, null, false,
                "Hey {user.mention}, welcome to {server.name}!",
                false, "Sorry to see you go, {user.name}..."
        );
    }
}
