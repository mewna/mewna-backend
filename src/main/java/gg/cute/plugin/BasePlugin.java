package gg.cute.plugin;

import gg.cute.Cute;
import lombok.Getter;
import org.slf4j.Logger;

import javax.inject.Inject;

/**
 * @author amy
 * @since 4/8/18.
 */
public class BasePlugin {
    @Getter
    @Inject
    private Logger logger;
    
    @Getter
    @Inject
    private Cute cute;
}
