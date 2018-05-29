package com.mewna.plugin.plugins.settings;

import com.mewna.data.CommandSettings;
import com.mewna.data.Database;
import com.mewna.data.PluginSettings;
import com.mewna.plugin.plugins.PluginEconomy;
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
@Table("settings_economy")
@Index("id")
@SuppressWarnings("unused")
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
    
    @Override
    public boolean validateSettings(final JSONObject data) {
        for(final String key : data.keySet()) {
            switch(key) {
                case "currencySymbol": {
                    final Optional<String> string = Optional.ofNullable(data.optString(key));
                    if(string.isPresent()) {
                        final String sym = string.get();
                        if(sym.length() > 16) {
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
        final EconomySettingsBuilder builder = toBuilder();
        try {
            // Trigger exception if not present
            data.getString("currencySymbol");
            String currencySymbol = data.optString("currencySymbol");
            if(currencySymbol == null || currencySymbol.isEmpty()) {
                currencySymbol = ":white_flower:";
            }
            builder.currencySymbol(currencySymbol);
            builder.commandSettings(commandSettingsFromJson(data));
            database.saveSettings(builder.build());
            return true;
        } catch(final JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}
