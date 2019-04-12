package com.mewna.data;

import com.mewna.Mewna;
import com.mewna.plugin.commands.CommandManager.CommandWrapper;
import io.sentry.Sentry;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Per-plugin settings. This encapsulates a set of {@link CommandSettings} as
 * well as other plugin-specific settings (ex. for event processing).
 * <p/>
 * Subclasses of this class should set things like ORM annotations to ensure
 * that everything works right.
 *
 * @author amy
 * @since 5/19/18.
 */
public interface PluginSettings {
    static <T> List<String> commandsOwnedByPlugin(final Class<T> cls) {
        return Mewna.getInstance().commandManager().getCommandsForPlugin(cls).stream().map(CommandWrapper::getBaseName)
                .distinct().collect(Collectors.toList());
    }
    
    default <T> Map<String, CommandSettings> generateCommandSettings(final Class<T> plugin) {
        final Map<String, CommandSettings> settings = new HashMap<>();
        commandsOwnedByPlugin(plugin).forEach(e -> settings.put(e, CommandSettings.base()));
        return settings;
    }
    
    String getId();
    
    Map<String, CommandSettings> getCommandSettings();
    
    PluginSettings refreshCommands();
    
    default PluginSettings otherRefresh() {
        return this;
    }
    
    /**
     * Validate the JSON data (against current settings if needed) to make sure
     * that we didn't get passed bad data.
     * <p/>
     * This method basically entirely exists so that we can validate incoming
     * config changes from the website. The idea is that each config has its
     * own needs and whatnot, so it makes more sense to validate it at a config
     * class level rather than trying to make it all fit into a single massive
     * "ConfigValidator" type class. More importantly, this also allows the
     * validation logic to be neatly separated into "modules" that should
     * hopefully make future maintenance easier for me.
     * <p/>
     * Ideally, implementations of this method would try to return as fast as
     * possible, ie. returning the moment a bad key is found rather than trying
     * to validate every single part of the JSON blob when not absolutely
     * needed.
     *
     * @param data The data to validate
     *
     * @return {@code true} if the data is valid, {@code false} otherwise.
     */
    boolean validateSettings(JsonObject data);
    
    /**
     * Actually updates the settings in the database. This may catch validation
     * errors that {@link #validateSettings(JsonObject)} and {@link #validate(JsonObject)}
     * did not catch, and will return false in that specific case.
     * <p/>
     * This should be pure <b>IN THE CASE OF FAILURE ONLY</b>. Specifically,
     * this method should <b>NOT</b> update anything in the database on
     * failure; the database should only ever be updated if the data is 100%
     * valid.
     *
     * @param database The database to update into. Passed here to as to avoid
     *                 singleton abuse.
     * @param data     The data to update and insert.
     *
     * @return {@code true} if the operation succeeded, {@code false} if some
     * invalid data still made it through.
     */
    boolean updateSettings(Database database, JsonObject data);
    
    /**
     * Actually does the validation. Will call {@link #validateSettings(JsonObject)}
     * after validating {@code commandSettings}
     *
     * @param data The data to validate
     *
     * @return {@code true} if the data is valid, {@code false} otherwise.
     */
    default boolean validate(final JsonObject data) {
        // validate commandSettings
        if(data.containsKey("commandSettings") && data.getJsonObject("commandSettings", null) != null) {
            final JsonObject commandSettings = data.getJsonObject("commandSettings");
            final Set<String> keys = commandSettings.fieldNames();
            for(final String key : keys) {
                final Optional<JsonObject> maybeCommand = Optional.ofNullable(commandSettings.getJsonObject(key, null));
                if(maybeCommand.isPresent()) {
                    try {
                        // If this fails, it's bad JSON or some shit, so reject it
                        maybeCommand.get().mapTo(CommandSettings.class);
                    } catch(final Exception e) {
                        return false;
                    }
                } else {
                    // Exit early due to bad command
                    return false;
                }
            }
        }
        // delegate to implementing class if needed
        return validateSettings(data);
    }
    
    default Map<String, CommandSettings> commandSettingsFromJson(final JsonObject data) {
        try {
            final Map<String, CommandSettings> commandSettings = new HashMap<>();
            
            if(data.containsKey("commandSettings")) {
                Optional.ofNullable(data.getJsonObject("commandSettings", null)).ifPresent(o -> {
                    for(final String key : o.fieldNames()) {
                        final Optional<JsonObject> maybeSettings = Optional.ofNullable(o.getJsonObject(key, null));
                        maybeSettings.ifPresent(s -> commandSettings.put(key,
                                new CommandSettings(s.getBoolean("enabled", true))));
                    }
                });
            }
            
            return commandSettings;
        } catch(final Exception e) {
            Sentry.capture(e);
            throw new RuntimeException(e);
        }
    }
}
