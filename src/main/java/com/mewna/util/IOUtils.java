package com.mewna.util;

import io.sentry.Sentry;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author amy
 * @since 10/26/18.
 */
public final class IOUtils {
    private IOUtils() {
    }
    
    public static void scan(@Nonnull @SuppressWarnings("SameParameterValue") final String path,
                            @Nonnull final Consumer<JarEntry> callback) {
        try {
            final URL url = IOUtils.class.getProtectionDomain().getCodeSource().getLocation();
            try(final InputStream is = url.openStream()) {
                final JarInputStream stream = new JarInputStream(is);
                JarEntry entry;
                while((entry = stream.getNextJarEntry()) != null) {
                    if(entry.getName().startsWith(path)) {
                        callback.accept(entry);
                    }
                }
            }
        } catch(final IOException e) {
            Sentry.capture(e);
            e.printStackTrace();
        }
    }
    
    public static byte[] readFully(@Nonnull final InputStream stream) throws IOException {
        final byte[] buffer = new byte[1024];
        try(final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int readAmount;
            while((readAmount = stream.read(buffer)) != -1) {
                bos.write(buffer, 0, readAmount);
            }
            return bos.toByteArray();
        }
    }
    
    @Nonnull
    public static String ip() {
        final String podIpEnv = System.getenv("POD_IP");
        if(podIpEnv != null) {
            return podIpEnv;
        } else {
            try {
                return Inet4Address.getLocalHost().getHostAddress();
            } catch(final UnknownHostException var3) {
                throw new IllegalStateException("DNS broken? Can't resolve localhost!", var3);
            }
        }
    }
}
