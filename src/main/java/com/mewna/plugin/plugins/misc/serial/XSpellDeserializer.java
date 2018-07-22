package com.mewna.plugin.plugins.misc.serial;

import com.google.gson.*;
import com.mewna.plugin.plugins.PluginMisc.XSpell;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author amy
 * @since 1/21/17.
 */
public class XSpellDeserializer implements JsonDeserializer<XSpell> {
    @Override
    public XSpell deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject o = json.getAsJsonObject();
        final String name = getOrNull(o, "name");
        final String level = getOrNull(o, "level");
        final String school = getOrNull(o, "school");
        final String time = getOrNull(o, "time");
        final String range = getOrNull(o, "range");
        final String components = getOrNull(o, "components");
        final String duration = getOrNull(o, "duration");
        final String classes = getOrNull(o, "classes");
        final List<String> text = new ArrayList<>();
        final JsonElement texts = o.get("text");
        if(texts.isJsonArray()) {
            final JsonArray arr = texts.getAsJsonArray();
            for(int i = 0; i < arr.size(); i++) {
                text.add(arr.get(i).getAsString());
            }
        } else {
            text.add(texts.getAsString());
        }
        final List<String> roll = new ArrayList<>();
        final JsonElement rolls = o.get("roll");
        if(rolls != null) {
            if(rolls.isJsonArray()) {
                final JsonArray arr = rolls.getAsJsonArray();
                for(int i = 0; i < arr.size(); i++) {
                    roll.add(arr.get(i).getAsString());
                }
            } else {
                roll.add(rolls.getAsString());
            }
        }
        return new XSpell(name, level, school, time, range, components, duration, classes, text, roll);
    }
    
    private String getOrNull(final JsonObject o, final String key) {
        final JsonElement elem = o.get(key);
        if(elem == null) {
            return "None";
        } else {
            return elem.getAsString();
        }
    }
}
