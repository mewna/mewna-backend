package com.mewna.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author amy
 * @since 5/19/18.
 */
@SuppressWarnings("unused")
public final class Templater {
    private final Map<String, String> variables = new HashMap<>();
    
    public Templater setVariable(final String name, final String value) {
        variables.put(name, value);
        return this;
    }
    
    public Templater unsetVariable(final String name) {
        variables.remove(name);
        return this;
    }
    
    public static Templater fromMap(final Map<String, String> map) {
        final Templater templater = new Templater();
        templater.variables.putAll(map);
        return templater;
    }
    
    public String render(String template) {
        if(template.isEmpty()) {
            throw new IllegalStateException("Can't render empty template!");
        }
        for(final Entry<String, String> variableEntry : variables.entrySet()) {
            template = template.replace('{' + variableEntry.getKey() + '}', variableEntry.getValue());
        }
        return template;
    }
    
}
