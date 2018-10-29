package com.mewna.plugin;

import com.mewna.Mewna;
import com.mewna.catnip.Catnip;
import com.mewna.data.Database;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.Random;

/**
 * @author amy
 * @since 4/8/18.
 */
@SuppressWarnings("unused")
@Accessors(fluent = true)
public abstract class BasePlugin {
    @Inject
    @Getter(AccessLevel.PROTECTED)
    private Logger logger;
    @Inject
    @Getter(AccessLevel.PROTECTED)
    private Mewna mewna;
    @Inject
    @Getter(AccessLevel.PROTECTED)
    private Database database;
    @Inject
    @Getter(AccessLevel.PROTECTED)
    private Random random;
    
    protected final Catnip catnip() {
        return mewna().catnip();
    }
}
