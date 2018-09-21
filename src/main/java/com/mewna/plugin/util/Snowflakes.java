package com.mewna.plugin.util;

import io.sentry.Sentry;
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
    @SuppressWarnings("StaticVariableOfConcreteClass")
    private static final InternalSnowflake snowflake = new InternalSnowflake(0L, 0L);
    
    private Snowflakes() {
    }
    
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    public static String getNewSnowflake() {
        if(System.getenv("FORCE_INTERNAL_SNOWFLAKES") != null) {
            return snowflake.nextId() + "";
        }
        try {
            @SuppressWarnings("ConstantConditions")
            final String snowflake = client.newCall(new Request.Builder().get().url(System.getenv("SNOWFLAKE_GENERATOR") + '/')
                    .build()).execute().body().string();
            return snowflake;
        } catch(final IOException e) {
            Sentry.capture(e);
            throw new RuntimeException(e);
        }
    }
}
