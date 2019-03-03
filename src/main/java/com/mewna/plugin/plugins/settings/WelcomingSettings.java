package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginWelcoming;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_welcoming")
@GIndex("id")
@SuppressWarnings("unused")
public class WelcomingSettings implements PluginSettings {
    private static final int DISCORD_MAX_MESSAGE_SIZE = 2000;
    @PrimaryKey
    private String id;
    private Map<String, CommandSettings> commandSettings;
    private String messageChannel;
    private String joinRoleId;
    private boolean enableWelcomeMessages;
    private String welcomeMessage = "Hey {user.mention}, welcome to {server.name}!";
    private boolean enableGoodbyeMessages;
    private String goodbyeMessage = "Sorry to see you go, {user.name}...";
    
    public WelcomingSettings(final String id) {
        this.id = id;
        commandSettings = generateCommandSettings(PluginWelcoming.class);
    }
    
    @Override
    public PluginSettings refreshCommands() {
        final Map<String, CommandSettings> oldSettings = new HashMap<>(commandSettings);
        final Map<String, CommandSettings> newSettings = generateCommandSettings(PluginWelcoming.class);
        newSettings.putAll(oldSettings);
        commandSettings.putAll(newSettings);
        return this;
    }
    
    // TODO: Validate channel existence
    
    @Override
    public boolean validateSettings(final JsonObject data) {
        for(final String key : data.fieldNames()) {
            switch(key) {
                case "welcomeMessage":
                case "goodbyeMessage": {
                    final Optional<String> maybeData = Optional.ofNullable(data.getString(key, null));
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
    
    @Override
    public boolean updateSettings(final Database database, final JsonObject data) {
        try {
            String messageChannel = data.getString("messageChannel", null);
            if(messageChannel != null && messageChannel.isEmpty()) {
                // non-null but empty should be nulled so that things work right
                messageChannel = null;
            }
            this.messageChannel = messageChannel;
            
            String joinRoleId = data.getString("joinRoleId", null);
            if(joinRoleId != null && joinRoleId.isEmpty()) {
                // non-null but empty should be nulled so that things work right
                joinRoleId = null;
            }
            this.joinRoleId = joinRoleId;
            enableWelcomeMessages = data.getBoolean("enableWelcomeMessages", false);
            enableGoodbyeMessages = data.getBoolean("enableGoodbyeMessages", false);
            // Trigger exception if not present
            data.getString("welcomeMessage");
            data.getString("goodbyeMessage");
            
            welcomeMessage = data.getString("welcomeMessage");
            goodbyeMessage = data.getString("goodbyeMessage");
            
            commandSettings.putAll(commandSettingsFromJson(data));
            database.saveSettings(this);
            return true;
        } catch(final Exception e) {
            return false;
        }
    }
}
