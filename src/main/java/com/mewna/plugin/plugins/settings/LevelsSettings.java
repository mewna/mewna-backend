package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginLevels;
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
import java.util.Set;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder(toBuilder = true)
@Table("settings_levels")
@Index("id")
@SuppressWarnings("unused")
public class LevelsSettings implements PluginSettings {
    private static final int DISCORD_MAX_MESSAGE_SIZE = 2000;
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    private final boolean levelsEnabled;
    private final boolean levelUpMessagesEnabled;
    private final boolean levelUpCards;
    private final String levelUpMessage;
    private Map<Integer, Set<String>> levelRoleRewards;
    
    public static LevelsSettings base(final String id) {
        final Map<String, CommandSettings> settings = new HashMap<>();
        PluginSettings.commandsOwnedByPlugin(PluginLevels.class).forEach(e -> settings.put(e, CommandSettings.base()));
        return new LevelsSettings(id, settings, false, true, true,
                "{user.name} leveled :up: to {level}! :tada:", new HashMap<>());
    }
    
    @Override
    public boolean validateSettings(final JSONObject data) {
        for(final String key : data.keySet()) {
            switch(key) {
                case "levelUpMessage": {
                    final Optional<String> maybeData = Optional.ofNullable(data.optString(key));
                    if(maybeData.isPresent()) {
                        final String s = maybeData.get();
                        if(s.isEmpty() || s.length() > DISCORD_MAX_MESSAGE_SIZE) {
                            return false;
                        }
                        break;
                    }
                    // Don't return false if no message, because some people may want cards only
                }
                default: {
                    break;
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean updateSettings(final Database database, final JSONObject data) {
        final LevelsSettingsBuilder builder = toBuilder();
        try {
            // Trigger exception if not present
            data.getString("levelUpMessage");
            String levelUpMessage = data.optString("levelUpMessage");
            if(levelUpMessage == null) {
                levelUpMessage = "";
            }
            builder.levelUpMessage(levelUpMessage);
            builder.levelsEnabled(data.optBoolean("levelsEnabled", false));
            builder.levelUpMessagesEnabled(data.optBoolean("levelUpMessagesEnabled", false));
            builder.levelUpCards(data.optBoolean("levelUpCards", false));
            // TODO: Role rewards go here
    
            builder.commandSettings(commandSettingsFromJson(data));
            database.saveSettings(builder.build());
            return true;
        } catch(final JSONException e) {
            return false;
        }
    }
}
