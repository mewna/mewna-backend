package com.mewna.util;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author amy
 * @since 10/26/18.
 */
public class TranslatorTest {
    @Test
    public void translate() {
        /*
        {
          "key": "value",
          "nest": {
            "key": "value 2",
            "nest_2": {
              "key": "value 3"
            }
          }
        }
         */
        final JsonObject translation =
                new JsonObject().put("key", "value")
                        .put("nest", new JsonObject()
                                .put("key", "value 2")
                                .put("nest_2", new JsonObject()
                                        .put("key", "value 3")));
        Translator.loadTranslation("en_US", translation);
        assertEquals("value", Translator.translate("en_US", "key"));
        assertEquals("value 2", Translator.translate("en_US", "nest.key"));
        assertEquals("value 3", Translator.translate("en_US", "nest.nest_2.key"));
    }
}