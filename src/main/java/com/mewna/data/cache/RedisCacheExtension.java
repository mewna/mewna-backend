package com.mewna.data.cache;

import com.mewna.catnip.extension.AbstractExtension;

/**
 * @author amy
 * @since 4/28/19.
 */
public class RedisCacheExtension extends AbstractExtension {
    private final String dsn;
    
    /**
     * @param dsn redis://[password@]host[:port][/databaseNumber]
     */
    public RedisCacheExtension(final String dsn) {
        super("redis cache");
        this.dsn = dsn;
    }
    
    @Override
    public void start() throws Exception {
        injectOptions(opts -> opts.cacheWorker(new MsgPackRedisCache(dsn)));
    }
}
