package com.mewna.plugin.event.plugin.twitch;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author amy
 * @since 6/20/18.
 */
public class TwitchStreamDataTest {
    @Test
    public void testInstantFromJson() {
        final String JSON_I = "{\"instant\": \"2018-06-18T12:56:20Z\"}";
        final Instant instant = Instant.parse("2018-06-18T12:56:20Z");
        assertTrue(true);
        
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule())
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module());
        try {
            final TestInstant testInstant = mapper.readValue(JSON_I, TestInstant.class);
            assertEquals(instant, testInstant.instant);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings("WeakerAccess")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class TestInstant {
        @JsonProperty("instant")
        @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        private Instant instant;
    }
}