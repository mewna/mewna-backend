package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.PluginSettings;
import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Value;

import java.util.Map;

/**
 * @author amy
 * @since 5/19/18.
 */
@Value
@Table("settings_welcoming")
@Index("id")
public class WelcomingSettings extends PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    private String welcomeChannel;
    private String joinRoleId;
    private boolean enableWelcomeMessages;
    private String welcomeMessage;
    private boolean enableGoodbyeMessages;
    private String goodbyeMessage;
}
