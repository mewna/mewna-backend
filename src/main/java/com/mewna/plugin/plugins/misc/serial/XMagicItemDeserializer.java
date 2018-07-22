package com.mewna.plugin.plugins.misc.serial;

import com.google.gson.*;
import com.mewna.plugin.plugins.PluginMisc.Modifier;
import com.mewna.plugin.plugins.PluginMisc.XMagicItem;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author amy
 * @since 1/21/17.
 */
public class XMagicItemDeserializer implements JsonDeserializer<XMagicItem> {
    @Override
    public XMagicItem deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject o = json.getAsJsonObject();
        final String name = getOrNull(o, "name");
        final String type = getOrNull(o, "type");
        final String weight = getOrNull(o, "weight");
        final String dmg1 = getOrNull(o, "dmg1");
        final String dmgType = getOrNull(o, "dmgType");
        final String property = getOrNull(o, "property");
        final String rarity = getOrNull(o, "rarity");
        
        final List<String> text = new ArrayList<>();
        final JsonArray textArray = o.get("text").getAsJsonArray();
        for(int i = 0; i < textArray.size(); i++) {
            text.add(textArray.get(i).getAsString());
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
        
        return new XMagicItem(name, type, weight, dmg1, dmgType, property, rarity, text, modifiers);
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
