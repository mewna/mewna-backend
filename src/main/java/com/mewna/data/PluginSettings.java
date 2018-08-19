package com.mewna.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.Mewna;
import com.mewna.plugin.CommandManager.CommandWrapper;
import com.mewna.plugin.plugins.PluginMusic;
import io.sentry.Sentry;
import org.json.JSONObject;

import java.io.IOException;
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
@SuppressWarnings("InterfaceMayBeAnnotatedFunctional")
public interface PluginSettings {
    ObjectMapper MAPPER = new ObjectMapper();
    
    static <T> List<String> commandsOwnedByPlugin(final Class<T> cls) {
        return Mewna.getInstance().getCommandManager().getCommandsForPlugin(cls).stream().map(CommandWrapper::getBaseName)
                .distinct().collect(Collectors.toList());
    }
    
    default Map<String, CommandSettings> generateCommandSettings() {
        final Map<String, CommandSettings> settings = new HashMap<>();
        commandsOwnedByPlugin(getClass()).forEach(e -> settings.put(e, CommandSettings.base()));
        return settings;
    }
    
    Map<String, CommandSettings> getCommandSettings();
    
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
    boolean validateSettings(JSONObject data);
    
    /**
     * Actually updates the settings in the database. This may catch validation
     * errors that {@link #validateSettings(JSONObject)} and {@link #validate(JSONObject)}
     * did not catch, and will return false in that specific case.
     * <p />
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
    boolean updateSettings(Database database, JSONObject data);
    
    /**
     * Actually does the validation. Will call {@link #validateSettings(JSONObject)}
     * after validating {@code commandSettings}
     *
     * @param data The data to validate
     *
     * @return {@code true} if the data is valid, {@code false} otherwise.
     */
    default boolean validate(final JSONObject data) {
        // validate commandSettings
        if(data.has("commandSettings") && !data.isNull("commandSettings")) {
            final Optional<JSONObject> maybeSettings = Optional.ofNullable(data.optJSONObject("commandSettings"));
            if(maybeSettings.isPresent()) {
                final JSONObject commandSettings = maybeSettings.get();
                final Set<String> keys = commandSettings.keySet();
                for(final String key : keys) {
                    final Optional<JSONObject> maybeCommand = Optional.ofNullable(commandSettings.optJSONObject(key));
                    if(maybeCommand.isPresent()) {
                        try {
                            // If this fails, it's bad JSON or some shit, so reject it
                            MAPPER.readValue(maybeCommand.get().toString(), CommandSettings.class);
                        } catch(final IOException e) {
                            return false;
                        }
                    } else {
                        // Exit early due to bad command
                        return false;
                    }
                }
            }
        }
        // delegate to implementing class if needed
        return validateSettings(data);
    }
    
    default Map<String, CommandSettings> commandSettingsFromJson(final JSONObject data) {
        try {
            final Map<String, CommandSettings> commandSettings = new HashMap<>();
    
            if(data.has("commandSettings")) {
                Optional.ofNullable(data.optJSONObject("commandSettings")).ifPresent(o -> {
                    for(final String key : o.keySet()) {
                        final Optional<JSONObject> maybeSettings = Optional.ofNullable(o.optJSONObject(key));
                        maybeSettings.ifPresent(s -> commandSettings.put(key, new CommandSettings(s.optBoolean("enabled", true))));
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
