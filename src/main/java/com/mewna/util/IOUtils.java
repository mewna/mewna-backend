package com.mewna.util;

import com.mewna.plugin.util.Renderer;
import io.sentry.Sentry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author amy
 * @since 10/26/18.
 */
public class IOUtils {
    public static void scan(@SuppressWarnings("SameParameterValue") final String path, final Consumer<JarEntry> callback) {
        try {
            final URL url = Renderer.class.getProtectionDomain().getCodeSource().getLocation();
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
    
    public static byte[] readFully(final InputStream stream) throws IOException {
        final byte[] buffer = new byte[1024];
        try(final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int readAmount;
            while((readAmount = stream.read(buffer)) != -1) {
                bos.write(buffer, 0, readAmount);
            }
            return bos.toByteArray();
        }
    }
}
