package com.mewna.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.Mewna;
import com.mewna.plugin.CommandManager.CommandWrapper;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    boolean validate(JSONObject data);
    
    default boolean checkValidate(final JSONObject data) {
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
        return validate(data);
    }
}
