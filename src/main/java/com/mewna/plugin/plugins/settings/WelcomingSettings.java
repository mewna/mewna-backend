package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginWelcoming;
import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder(toBuilder = true)
@Table("settings_welcoming")
@Index("id")
@SuppressWarnings("unused")
public class WelcomingSettings implements PluginSettings {
    private static final int DISCORD_MAX_MESSAGE_SIZE = 2000;
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    private String messageChannel;
    private String joinRoleId;
    private boolean enableWelcomeMessages;
    private String welcomeMessage;
    private boolean enableGoodbyeMessages;
    private String goodbyeMessage;
    
    public static WelcomingSettings base(final String id) {
        final Map<String, CommandSettings> settings = new HashMap<>();
        PluginSettings.commandsOwnedByPlugin(PluginWelcoming.class).forEach(e -> settings.put(e, CommandSettings.base()));
        return new WelcomingSettings(id, settings, null, null, false,
                "Hey {user.mention}, welcome to {server.name}!", false,
                "Sorry to see you go, {user.name}...");
    }
    
    @Override
    public boolean validateSettings(final JSONObject data) {
        for(final String key : data.keySet()) {
            switch(key) {
                case "welcomeMessage":
                case "goodbyeMessage": {
                    final Optional<String> maybeData = Optional.ofNullable(data.optString(key));
                    if(maybeData.isPresent()) {
                        final String s = maybeData.get();
                        if(s.isEmpty() || s.length() > DISCORD_MAX_MESSAGE_SIZE) {
                            return false;
                        }
                        break;
                    } else {
                        // must have a message for these
                        // if not wanted, just hit the toggle switch :V
                        return false;
                    }
                }
                default: {
                    break;
                }
            }
        }
        return true;
    }
    /*
    messageChannel
    joinRoleId
    
    enableWelcomeMessages
    enableGoodbyeMessages
    
    welcomeMessage
    goodbyeMessage
    */
    
    @Override
    public boolean updateSettings(final Database database, final JSONObject data) {
        final WelcomingSettingsBuilder builder = toBuilder();
        try {
            String messageChannel = data.optString("messageChannel");
            if(messageChannel != null && messageChannel.isEmpty()) {
                // non-null but empty should be nulled so that things work right
                messageChannel = null;
            }
            builder.messageChannel(messageChannel);
            
            String joinRoleId = data.optString("joinRoleId");
            if(joinRoleId != null && joinRoleId.isEmpty()) {
                // non-null but empty should be nulled so that things work right
                joinRoleId = null;
            }
            builder.joinRoleId(joinRoleId);
            builder.enableWelcomeMessages(data.optBoolean("enableWelcomeMessages", false));
            builder.enableGoodbyeMessages(data.optBoolean("enableGoodbyeMessages", false));
            // Trigger exception if not present
            data.getString("welcomeMessage");
            data.getString("goodbyeMessage");
    
            builder.welcomeMessage(data.getString("welcomeMessage"));
            builder.goodbyeMessage(data.getString("goodbyeMessage"));
            
            builder.commandSettings(commandSettingsFromJson(data));
            database.saveSettings(builder.build());
            return true;
        } catch(final Exception e) {
            return false;
        }
    }
}
