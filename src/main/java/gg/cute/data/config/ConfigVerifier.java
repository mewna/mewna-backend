package gg.cute.data.config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static gg.cute.data.config.Constraints.VALUE_NOT_NULL;

/**
 * @author amy
 * @since 5/9/18.
 */
@SuppressWarnings("unused")
public class ConfigVerifier {
    private final Map<Class<?>, Map<Field, Config>> constraints = new ConcurrentHashMap<>();
    
    public <T> Map<String, List<String>> verify(final Class<T> c, final JSONObject data) {
        if(!constraints.containsKey(c)) {
            // Map it
            constraints.put(c, new ConcurrentHashMap<>());
            for(final Field field : c.getDeclaredFields()) {
                field.setAccessible(true);
                if(field.isAnnotationPresent(Config.class)) {
                    final Config constraint = field.getDeclaredAnnotation(Config.class);
                    constraints.get(c).put(field, constraint);
                }
            }
        }
        // Verify
        final Map<Field, Config> fieldConstraints = constraints.get(c);
        final Map<String, List<String>> badValues = new HashMap<>();
        fieldConstraints.forEach((key, value) -> {
            if(data.has(value.name())) {
                if(data.isNull(value.name())) {
                    mapListAdd(badValues, value.name(), VALUE_NOT_NULL.name());
                    return;
                }
                final Object dataObject;
                try {
                    dataObject = data.get(value.name());
                } catch(final JSONException e) {
                    mapListAdd(badValues, value.name(), VALUE_NOT_NULL.name());
                    return;
                }
                for(final Constraints con : value.constraints()) {
                    try {
                        switch(con) {
                            case TYPE_STRING: {
                                if(!(dataObject instanceof String)) {
                                    mapListAdd(badValues, value.name(), con.name());
                                }
                                break;
                            }
                            case TYPE_BOOLEAN: {
                                boolean isBool;
                                try {
                                    data.getBoolean(value.name());
                                    isBool = true;
                                } catch(final Exception ignored) {
                                    isBool = false;
                                }
                                if(!isBool) {
                                    mapListAdd(badValues, value.name(), con.name());
                                }
                                break;
                            }
                            case TYPE_MAP: {
                                if(!(dataObject instanceof JSONObject)) {
                                    mapListAdd(badValues, value.name(), con.name());
                                }
                                break;
                            }
                            case TYPE_LIST: {
                                if(!(dataObject instanceof JSONArray)) {
                                    mapListAdd(badValues, value.name(), con.name());
                                }
                                break;
                            }
                            case VALUE_NOT_NULL: {
                                if(dataObject == null) {
                                    mapListAdd(badValues, value.name(), con.name());
                                }
                                break;
                            }
                            case VALUE_STRING_NOT_EMPTY: {
                                if(!(dataObject instanceof String)) {
                                    mapListAdd(badValues, value.name(), con.name());
                                } else {
                                    final String s = (String) dataObject;
                                    if(s.isEmpty()) {
                                        mapListAdd(badValues, value.name(), con.name());
                                    }
                                }
                                break;
                            }
                            case STRING_LEN_8: {
                                if(!(dataObject instanceof String)) {
                                    mapListAdd(badValues, value.name(), con.name());
                                } else {
                                    @SuppressWarnings("TypeMayBeWeakened")
                                    final String s = (String) dataObject;
                                    if(s.length() > 8) {
                                        mapListAdd(badValues, value.name(), con.name());
                                    }
                                }
                                break;
                            }
                            case STRING_LEN_16: {
                                if(!(dataObject instanceof String)) {
                                    mapListAdd(badValues, value.name(), con.name());
                                } else {
                                    @SuppressWarnings("TypeMayBeWeakened")
                                    final String s = (String) dataObject;
                                    if(s.length() > 16) {
                                        mapListAdd(badValues, value.name(), con.name());
                                    }
                                }
                                break;
                            }
                            case STRING_LEN_32: {
                                if(!(dataObject instanceof String)) {
                                    mapListAdd(badValues, value.name(), con.name());
                                } else {
                                    @SuppressWarnings("TypeMayBeWeakened")
                                    final String s = (String) dataObject;
                                    if(s.length() > 32) {
                                        mapListAdd(badValues, value.name(), con.name());
                                    }
                                }
                                break;
                            }
                            case STRING_LEN_2K: {
                                if(!(dataObject instanceof String)) {
                                    mapListAdd(badValues, value.name(), con.name());
                                } else {
                                    @SuppressWarnings("TypeMayBeWeakened")
                                    final String s = (String) dataObject;
                                    if(s.length() > 2_000) {
                                        mapListAdd(badValues, value.name(), con.name());
                                    }
                                }
                                break;
                            }
                            default: {
                                throw new IllegalArgumentException("Unknown constraint: " + con.name());
                            }
                        }
                    } catch(final Exception e) {
                        mapListAdd(badValues, value.name(), "NOT_PARSEABLE");
                    }
                }
            }
        });
        return badValues;
    }
    
    public <T> T update(final T data, final JSONObject update) {
        if(!constraints.containsKey(data.getClass())) {
            throw new IllegalArgumentException("No constraints mapped for " + data.getClass().getName()
                    + "! You probably forgot to verify() your data first...");
        }
        
        // Assume that it's been verify()'d
        final Map<Field, Config> fields = constraints.get(data.getClass());
        fields.forEach((k, v) -> {
            if(update.has(v.name())) {
                try {
                    k.setAccessible(true);
                    k.set(data, update.get(v.name()));
                } catch(final IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        
        return data;
    }
    
    private <E, T> void mapListAdd(final Map<E, List<T>> map, final E key, final T value) {
        if(!map.containsKey(key)) {
            map.put(key, new ArrayList<>());
        }
        map.get(key).add(value);
    }
}
