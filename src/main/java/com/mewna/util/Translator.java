package com.mewna.util;

import io.sentry.Sentry;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 10/26/18.
 */
@SuppressWarnings("unused")
public final class Translator {
    private static final Map<String, JsonObject> LANGS = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(Translator.class);
    private static boolean PRELOADED;
    
    private Translator() {
    }
    
    public static void preload() {
        if(PRELOADED) {
            return;
        }
        PRELOADED = true;
        
        IOUtils.scan("lang", e -> {
            if(e.getName().endsWith(".json")) {
                try(final InputStream is = CacheUtil.class.getResourceAsStream("/lang/" + e.getName())) {
                    final String json = new String(IOUtils.readFully(is));
                    loadTranslation(e.getName().replace(".json", ""), new JsonObject(json));
                } catch(final IOException err) {
                    Sentry.capture(err);
                    throw new RuntimeException(err);
                }
            }
        });
        LOGGER.info("Loaded {} translations.", LANGS.size());
    }
    
    static void loadTranslation(@Nonnull final String lang, @Nonnull final JsonObject translation) {
        LANGS.put(lang, translation);
    }
    
    public static String translate(@Nonnull final String lang, @Nonnull final String key) {
        if(LANGS.containsKey(lang) && !key.isEmpty()) {
            final JsonObject translations = LANGS.get(lang);
            final String[] split = key.split("\\.");
            if(split.length == 1) {
                if(translations.containsKey(key)) {
                    return translations.getString(key);
                } else {
                    // Try to fetch with en_US
                    try {
                        return translate("en_US", key);
                    } catch(final Exception e) {
                        throw new IllegalArgumentException('`' + key + "` is not a valid key for `" + lang + "`!");
                    }
                }
            } else {
                try {
                    JsonObject target = translations;
                    for(int i = 0; i <= split.length - 2; i++) {
                        target = target.getJsonObject(split[i]);
                    }
                    return target.getString(split[split.length - 1]);
                } catch(final Exception e) {
                    throw new IllegalArgumentException('`' + key + "` is not a valid key for `" + lang + "`!", e);
                }
            }
        } else {
            throw new IllegalArgumentException('`' + lang + "` is not a valid language!");
        }
    }
    
    /**
     * Shortcut to {@link #translate(String, String)}
     */
    public static String $(@Nonnull final String lang, @Nonnull final String key) {
        return translate(lang, key);
    }
}
