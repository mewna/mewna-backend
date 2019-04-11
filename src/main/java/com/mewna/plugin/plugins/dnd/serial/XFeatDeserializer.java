package com.mewna.plugin.plugins.dnd.serial;

import com.google.gson.*;
import com.mewna.plugin.plugins.PluginMisc.Modifier;
import com.mewna.plugin.plugins.PluginMisc.XFeat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author amy
 * @since 1/22/17.
 */
public class XFeatDeserializer implements JsonDeserializer<XFeat> {
    @Override
    public XFeat deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject o = json.getAsJsonObject();
        final String name = getOrNull(o, "name");
        final String prereq = getOrNull(o, "prerequisite");
        final List<String> text = new ArrayList<>();
        final JsonElement textElem = o.get("text");
        if(textElem != null) {
            if(textElem.isJsonArray()) {
                final JsonArray jsonArray = textElem.getAsJsonArray();
                for(int i = 0; i < jsonArray.size(); i++) {
                    text.add(jsonArray.get(i).getAsString());
                }
            } else {
                text.add(textElem.getAsString());
            }
        }
        
        final List<Modifier> modifiers = new ArrayList<>();
        final JsonElement modifierElement = o.get("modifier");
        
        if(modifierElement != null) {
            if(modifierElement.isJsonArray()) {
                final JsonArray jsonArray = modifierElement.getAsJsonArray();
                for(int i = 0; i < jsonArray.size(); i++) {
                    final JsonObject elem = jsonArray.get(i).getAsJsonObject();
                    modifiers.add(new Modifier(elem.get("_category").getAsString(), elem.get("__text").getAsString()));
                }
            } else {
                // Assume object
                final JsonObject elem = modifierElement.getAsJsonObject();
                modifiers.add(new Modifier(elem.get("_category").getAsString(), elem.get("__text").getAsString()));
            }
        }
        return new XFeat(name, prereq, text, modifiers);
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
