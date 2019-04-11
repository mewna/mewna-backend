package com.mewna.plugin.plugins.dnd.serial;

import com.google.gson.*;
import com.mewna.plugin.plugins.PluginMisc.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author amy
 * @since 1/23/17.
 */
public class XRaceDeserializer implements JsonDeserializer<XRace> {
    @Override
    public XRace deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject o = json.getAsJsonObject();
        final String name = getOrNull(o, "name");
        final String size = getOrNull(o, "size");
        final String speed = getOrNull(o, "speed");
        final String ability = getOrNull(o, "ability");
        final String proficiency = getOrNull(o, "proficiency");
        
        final List<Trait> traits = new ArrayList<>();
        final JsonElement t = o.get("trait");
        if(t != null) {
            if(t.isJsonArray()) {
                final JsonArray arr = t.getAsJsonArray();
                for(int i = 0; i < arr.size(); i++) {
                    final JsonObject tr = arr.get(i).getAsJsonObject();
                    final String tname = getOrNull(tr, "name");
                    final List<String> ttxt = new ArrayList<>();
                    final JsonElement txt = tr.get("text");
                    if(txt.isJsonArray()) {
                        final JsonArray txtarr = txt.getAsJsonArray();
                        for(int j = 0; j < txtarr.size(); j++) {
                            ttxt.add(txtarr.get(j).getAsString());
                        }
                    } else {
                        ttxt.add(txt.getAsString());
                    }
                    traits.add(new Trait(tname, ttxt));
                }
            } else {
                // Assume object
                final JsonObject tr = t.getAsJsonObject();
                final String tname = getOrNull(tr, "name");
                final List<String> ttxt = new ArrayList<>();
                final JsonElement txt = tr.get("text");
                if(txt.isJsonArray()) {
                    final JsonArray txtarr = txt.getAsJsonArray();
                    for(int j = 0; j < txtarr.size(); j++) {
                        ttxt.add(txtarr.get(j).getAsString());
                    }
                } else {
                    ttxt.add(txt.getAsString());
                }
                traits.add(new Trait(tname, ttxt));
            }
        }
        
        return new XRace(name, size, speed, ability, proficiency, traits);
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
