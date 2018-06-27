package com.mewna.plugin.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;

/**
 * @author amy
 * @since 6/26/18.
 */
public final class Snowflakes {
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    private static final OkHttpClient client = new OkHttpClient.Builder().build();
    
    private Snowflakes() {
    }
    
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    public static String getNewSnowflake() {
        try {
            @SuppressWarnings("ConstantConditions")
            final String snowflake = client.newCall(new Request.Builder().get().url(System.getenv("SNOWFLAKE_GENERATOR") + '/')
                    .build()).execute().body().string();
            return snowflake;
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
