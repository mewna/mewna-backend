package com.mewna.plugin.plugins.dnd.serial;

import com.google.gson.*;
import com.mewna.plugin.plugins.PluginMisc.XItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author amy
 * @since 1/22/17.
 */
public class XItemDeserializer implements JsonDeserializer<XItem> {
    @Override
    public XItem deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject o = json.getAsJsonObject();
        final String name = getOrNull(o, "name");
        final String type = getOrNull(o, "type");
        final String value = getOrNull(o, "value");
        final String weight = getOrNull(o, "weight");
        final String dmg1 = getOrNull(o, "dmg1");
        final String dmg2 = getOrNull(o, "dmg2");
        final String dmgType = getOrNull(o, "dmgType");
        final String property = getOrNull(o, "property");
        final String range = getOrNull(o, "range");
        final List<String> roll = new ArrayList<>();
        final JsonElement rollElem = o.get("roll");
        if(rollElem != null) {
            if(rollElem.isJsonPrimitive()) {
                roll.add(rollElem.getAsString());
            } else {
                // Assume array
                final JsonArray arr = rollElem.getAsJsonArray();
                for(int i = 0; i < arr.size(); i++) {
                    roll.add(arr.get(i).getAsString());
                }
            }
        }
        
        final List<String> text = new ArrayList<>();
        final JsonElement textElem = o.get("text");
        if(textElem != null) {
            if(textElem.isJsonPrimitive()) {
                roll.add(textElem.getAsString());
            } else {
                // Assume array
                final JsonArray arr = textElem.getAsJsonArray();
                for(int i = 0; i < arr.size(); i++) {
                    text.add(arr.get(i).getAsString());
                }
            }
        }
        return new XItem(name, type, value, weight, dmg1, dmg2, dmgType, property, range, roll, text);
    }
    
    @SuppressWarnings("SameParameterValue")
    private String getOrNull(final JsonObject o, final String key) {
        final JsonElement elem = o.get(key);
        if(elem == null) {
            return "None";
        } else {
            return elem.getAsString();
        }
    }
}
