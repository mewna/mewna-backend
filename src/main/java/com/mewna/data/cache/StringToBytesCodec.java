package com.mewna.data.cache;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.ToByteBufEncoder;
import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author amy
 * @since 4/28/19.
 */
public class StringToBytesCodec implements RedisCodec<String, byte[]>, ToByteBufEncoder<String, byte[]> {
    private static final byte[] EMPTY = new byte[0];
    
    private static byte[] getBytes(final ByteBuffer buffer) {
        final var remaining = buffer.remaining();
        if(remaining == 0) {
            return EMPTY;
        }
        final var b = new byte[remaining];
        buffer.get(b);
        return b;
    }
    
    @Override
    public String decodeKey(final ByteBuffer bytes) {
        return new String(getBytes(bytes), Charset.defaultCharset());
    }
    
    @Override
    public byte[] decodeValue(final ByteBuffer bytes) {
        return getBytes(bytes);
    }
    
    @Override
    public ByteBuffer encodeKey(final String key) {
        if(key == null) {
            return ByteBuffer.wrap(EMPTY);
        }
        
        return ByteBuffer.wrap(key.getBytes(Charset.defaultCharset()));
    }
    
    @Override
    public ByteBuffer encodeValue(final byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }
    
    @Override
    public void encodeKey(final String key, final ByteBuf target) {
        target.writeBytes(key.getBytes(Charset.defaultCharset()));
    }
    
    @Override
    public void encodeValue(final byte[] bytes, final ByteBuf target) {
        target.writeBytes(bytes);
    }
    
    @Override
    public int estimateSize(final Object keyOrValue) {
        if(keyOrValue == null) {
            return 0;
        } else if(keyOrValue instanceof String) {
            return ((String) keyOrValue).getBytes(Charset.defaultCharset()).length;
        } else if(keyOrValue instanceof byte[]) {
            return ((byte[]) keyOrValue).length;
        } else {
            return 0;
        }
    }
}
