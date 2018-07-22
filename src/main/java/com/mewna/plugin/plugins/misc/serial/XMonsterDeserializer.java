package com.mewna.plugin.plugins.misc.serial;

import com.google.gson.*;
import com.mewna.plugin.plugins.PluginMisc.Trait;
import com.mewna.plugin.plugins.PluginMisc.XMonster;
import com.mewna.plugin.plugins.PluginMisc.XMonster.Action;
import com.mewna.plugin.plugins.PluginMisc.XMonster.Legendary;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @author amy
 * @since 1/21/17.
 */
public class XMonsterDeserializer implements JsonDeserializer<XMonster> {
    @Override
    public XMonster deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
        final JsonObject o = json.getAsJsonObject();
        final String name = getOrNull(o, "name");
        final String size = getOrNull(o, "size");
        final String type = getOrNull(o, "type");
        final String alignment = getOrNull(o, "alignment");
        final String ac = getOrNull(o, "ac");
        final String hp = getOrNull(o, "hp");
        final String speed = getOrNull(o, "speed");
        final String strength = getOrNull(o, "str");
        final String dexterity = getOrNull(o, "dex");
        final String constitution = getOrNull(o, "con");
        final String intelligence = getOrNull(o, "int");
        final String wisdom = getOrNull(o, "wis");
        final String charisma = getOrNull(o, "cha");
        final String save = getOrNull(o, "save");
        final String skill = getOrNull(o, "skill");
        final String resist = getOrNull(o, "resist");
        final String immune = getOrNull(o, "immune");
        final String conditionImmune = getOrNull(o, "conditionImmune");
        final String senses = getOrNull(o, "senses");
        final String passive = getOrNull(o, "passive");
        final String languages = getOrNull(o, "languages");
        final String cr = getOrNull(o, "cr");
        
        final List<Trait> traits = new ArrayList<>();
        final JsonElement traitWrapper = o.get("trait");
        if(traitWrapper != null) {
            if(traitWrapper.isJsonObject()) {
                final JsonObject object = traitWrapper.getAsJsonObject();
                traits.add(processTrait(object));
            } else {
                // Probably an array
                final JsonArray jsonArray = traitWrapper.getAsJsonArray();
                for(int i = 0; i < jsonArray.size(); i++) {
                    traits.add(processTrait(jsonArray.get(i).getAsJsonObject()));
                }
            }
        }
        
        final List<Action> actions = new ArrayList<>();
        final JsonElement actionWrapper = o.get("action");
        if(actionWrapper != null) {
            if(actionWrapper.isJsonObject()) {
                final JsonObject object = actionWrapper.getAsJsonObject();
                actions.add(processAction(object));
            } else {
                // Probably an array
                final JsonArray jsonArray = actionWrapper.getAsJsonArray();
                for(int i = 0; i < jsonArray.size(); i++) {
                    actions.add(processAction(jsonArray.get(i).getAsJsonObject()));
                }
            }
        }
        
        final List<Legendary> legendaries = new ArrayList<>();
        final JsonElement legendaryWrapper = o.get("legendary");
        if(legendaryWrapper != null) {
            if(legendaryWrapper.isJsonObject()) {
                final JsonObject object = legendaryWrapper.getAsJsonObject();
                legendaries.add(processLegendary(object));
            } else {
                // Probably an array
                final JsonArray jsonArray = legendaryWrapper.getAsJsonArray();
                for(int i = 0; i < jsonArray.size(); i++) {
                    legendaries.add(processLegendary(jsonArray.get(i).getAsJsonObject()));
                }
            }
        }
        
        return new XMonster(name, size, type, alignment, ac, hp, speed, strength, dexterity, constitution, intelligence,
                wisdom, charisma, save, skill, resist, immune, conditionImmune, senses, passive, languages, cr, traits,
                actions, legendaries);
    }
    
    private Legendary processLegendary(final JsonObject object) {
        final List<String> text = new ArrayList<>();
        final JsonElement jText = object.get("text");
        if(jText.isJsonPrimitive()) {
            text.add(jText.getAsString());
        } else {
            final JsonArray jsonArray = jText.getAsJsonArray();
            for(int i = 0; i < jsonArray.size(); i++) {
                text.add(jsonArray.get(i).getAsString());
            }
        }
        final List<String> attack = new ArrayList<>();
        final JsonElement elem = object.get("attack");
        if(elem != null) {
            if(elem.isJsonArray()) {
                final JsonArray arr = elem.getAsJsonArray();
                for(int i = 0; i < arr.size(); i++) {
                    attack.add(arr.get(i).getAsString());
                }
            } else {
                attack.add(elem.getAsString());
            }
        }
        return new Legendary(getOrNull(object, "name"), text, attack);
    }
    
    private Action processAction(final JsonObject object) {
        final List<String> text = new ArrayList<>();
        final JsonElement jText = object.get("text");
        if(jText.isJsonPrimitive()) {
            text.add(jText.getAsString());
        } else {
            final JsonArray jsonArray = jText.getAsJsonArray();
            for(int i = 0; i < jsonArray.size(); i++) {
                text.add(jsonArray.get(i).getAsString());
            }
        }
        final List<String> attack = new ArrayList<>();
        final JsonElement elem = object.get("attack");
        if(elem != null) {
            if(elem.isJsonArray()) {
                final JsonArray arr = elem.getAsJsonArray();
                for(int i = 0; i < arr.size(); i++) {
                    attack.add(arr.get(i).getAsString());
                }
            } else {
                attack.add(elem.getAsString());
            }
        }
        return new Action(getOrNull(object, "name"), text, attack);
    }
    
    private Trait processTrait(final JsonObject object) {
        final List<String> text = new ArrayList<>();
        final JsonElement jText = object.get("text");
        if(jText.isJsonPrimitive()) {
            text.add(jText.getAsString());
        } else {
            final JsonArray jsonArray = jText.getAsJsonArray();
            for(int i = 0; i < jsonArray.size(); i++) {
                text.add(jsonArray.get(i).getAsString());
            }
        }
        return new Trait(getOrNull(object, "name"), text);
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
