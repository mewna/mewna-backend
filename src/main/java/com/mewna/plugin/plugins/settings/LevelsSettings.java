package com.mewna.plugin.plugins.settings;

import com.mewna.catnip.entity.guild.Role;
import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.DiscordCache;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginLevels;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_levels")
@GIndex("id")
@SuppressWarnings("unused")
public class LevelsSettings implements PluginSettings {
    private static final int DISCORD_MAX_MESSAGE_SIZE = 2000;
    @PrimaryKey
    private String id;
    private Map<String, CommandSettings> commandSettings;
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
        commandSettings = generateCommandSettings(PluginLevels.class);
    }
    
    @Override
    public PluginSettings refreshCommands() {
        final Map<String, CommandSettings> oldSettings = new HashMap<>(commandSettings);
        final Map<String, CommandSettings> newSettings = generateCommandSettings(PluginLevels.class);
        newSettings.putAll(oldSettings);
        commandSettings.putAll(newSettings);
        return this;
    }
    
    @Override
    public PluginSettings otherRefresh() {
        final Collection<Role> e = DiscordCache.roles(id);
        final List<String> roles = e.stream().map(Role::id).collect(Collectors.toList());
        
        final Collection<String> bad = new ArrayList<>();
        levelRoleRewards.keySet().forEach(r -> {
            if(!roles.contains(r)) {
                bad.add(r);
            }
        });
        bad.forEach(levelRoleRewards::remove);
        return this;
    }
    
    @Override
    public boolean validateSettings(final JsonObject data) {
        for(final String key : data.fieldNames()) {
            //noinspection SwitchStatementWithTooFewBranches
            switch(key) {
                case "levelUpMessage": {
                    final Optional<String> maybeData = Optional.ofNullable(data.getString(key, null));
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
    public boolean updateSettings(final Database database, final JsonObject data) {
        try {
            // Trigger exception if not present
            data.getString("levelUpMessage");
            String levelUpMessage = data.getString("levelUpMessage", null);
            if(levelUpMessage == null) {
                levelUpMessage = "";
            }
            this.levelUpMessage = levelUpMessage;
            levelsEnabled = data.getBoolean("levelsEnabled", false);
            levelUpMessagesEnabled = data.getBoolean("levelUpMessagesEnabled", false);
            removePreviousRoleRewards = data.getBoolean("removePreviousRoleRewards", false);
            
            // Basically just copy the object into a map as-is, converting data types to make sure it works
            final JsonObject rewards = data.getJsonObject("levelRoleRewards");
            final Map<String, Long> roleRewards = new HashMap<>();
            for(final String key : rewards.fieldNames()) {
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
        } catch(final Exception e) {
            return false;
        }
    }
}
