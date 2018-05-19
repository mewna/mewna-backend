package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.PluginSettings;
import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Value;

import java.util.Map;
import java.util.Set;

/**
 * @author amy
 * @since 5/19/18.
 */
@Value
@Table("settings_levels")
@Index("id")
public class LevelSettings extends PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    private final boolean levelsEnabled;
    private final boolean levelUpMessagesEnabled;
    private final boolean levelUpCards;
    private final String levelUpMessage;
    private Map<Integer, Set<String>> levelRoleRewards;
}
