package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginEconomy;
import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 5/19/18.
 */
@Getter
@Setter
@Accessors(chain = true)
@Builder
@Table("settings_economy")
@Index("id")
public class EconomySettings implements PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    private final String currencySymbol;
    
    public static EconomySettings base(final String id) {
        final Map<String, CommandSettings> settings = new HashMap<>();
        PluginSettings.commandsOwnedByPlugin(PluginEconomy.class).forEach(e -> settings.put(e, CommandSettings.base()));
        return new EconomySettings(id, settings, ":white_flower:");
    }
}
