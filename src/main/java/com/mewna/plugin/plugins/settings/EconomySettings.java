package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginBehaviour;
import com.mewna.plugin.plugins.PluginEconomy;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@Accessors(chain = true)
@Table("settings_economy")
@GIndex("id")
@SuppressWarnings("unused")
public class EconomySettings implements PluginSettings {
    @PrimaryKey
    private final String id;
    private final Map<String, CommandSettings> commandSettings;
    private String currencySymbol = ":white_flower:";
    
    public EconomySettings(final String id) {
        this.id = id;
        commandSettings = generateCommandSettings(PluginEconomy.class);
    }
    
    @Override
    public PluginSettings refreshCommands() {
        final Map<String, CommandSettings> oldSettings = new HashMap<>(commandSettings);
        final Map<String, CommandSettings> newSettings = generateCommandSettings(PluginEconomy.class);
        newSettings.putAll(oldSettings);
        commandSettings.putAll(newSettings);
        return this;
    }
    
    @Override
    public boolean validateSettings(final JSONObject data) {
        for(final String key : data.keySet()) {
            switch(key) {
                case "currencySymbol": {
                    final Optional<String> string = Optional.ofNullable(data.optString(key));
                    if(string.isPresent()) {
                        final String sym = string.get();
                        if(sym.length() > 64) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                    break;
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
            data.getString("currencySymbol");
            String currencySymbol = data.optString("currencySymbol");
            if(currencySymbol == null || currencySymbol.isEmpty()) {
                currencySymbol = ":white_flower:";
            }
            this.currencySymbol = currencySymbol;
            commandSettings.putAll(commandSettingsFromJson(data));
            database.saveSettings(this);
            return true;
        } catch(final JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}
