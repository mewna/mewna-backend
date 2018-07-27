package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_levels")
@GIndex("id")
@SuppressWarnings("unused")
public class LevelsSettings implements PluginSettings {
    private static final int DISCORD_MAX_MESSAGE_SIZE = 2000;
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    private boolean levelsEnabled;
    private boolean levelUpMessagesEnabled = true;
    private boolean removePreviousRoleRewards = true;
    private String levelUpMessage = "{user.name} leveled :up: to level {level}! :tada:";
    /**
     * Maps role ids to levels. I know it could be done differently. It was
     * done this way because doing a {@code Map<Long, Set<String>>} was SOMEHOW
     * causing issues in the JS frontend part of things. idfk HOW, but it did.
     */
    private Map<String, Long> levelRoleRewards = new HashMap<>();
    
    public LevelsSettings(final String id) {
        this.id = id;
        commandSettings = generateCommandSettings();
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
        try {
            // Trigger exception if not present
            data.getString("levelUpMessage");
            String levelUpMessage = data.optString("levelUpMessage");
            if(levelUpMessage == null) {
                levelUpMessage = "";
            }
            this.levelUpMessage = levelUpMessage;
            levelsEnabled = data.optBoolean("levelsEnabled", false);
            levelUpMessagesEnabled = data.optBoolean("levelUpMessagesEnabled", false);
            removePreviousRoleRewards = data.optBoolean("removePreviousRoleRewards", false);
            
            // Basically just copy the object into a map as-is, converting data types to make sure it works
            final JSONObject rewards = data.getJSONObject("levelRoleRewards");
            final Map<String, Long> roleRewards = new HashMap<>();
            for(final String key : rewards.keySet()) {
                roleRewards.put(key, rewards.getLong(key));
            }
            // Clean out empty levels
            final Collection<String> remove = new ArrayList<>();
            roleRewards.forEach((k, v) -> {
                if(v == 0) {
                    remove.add(k);
                }
            });
            remove.forEach(roleRewards::remove);
            levelRoleRewards.clear();
            levelRoleRewards.putAll(roleRewards);
            commandSettings.putAll(commandSettingsFromJson(data));
            database.saveSettings(this);
            return true;
        } catch(final JSONException e) {
            return false;
        }
    }
}
