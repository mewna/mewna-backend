package gg.cute.data;

import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Value;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    
    private String customPrefix;
    private Map<String, CommandSettings> commandToggles;
    
    // Twitch notification settings
    
    private String twitchWebhook;
    private Set<String> streamers;
    
    // Economy settings
    
    private String currencySymbol;
    
    // Levels settings
    
    private boolean levelsEnabled;
    private boolean levelUpMessagesEnabled;
    private boolean levelUpCards;
    private String levelUpMessage;
    private Map<Integer, Set<String>> roleRewards;
    
    // Welcoming settings
    
    private String welcomeChannel;
    private String joinRoleId;
    private boolean enableWelcomeMessages;
    private String welcomeMessage;
    private boolean enableGoodbyeMessages;
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
