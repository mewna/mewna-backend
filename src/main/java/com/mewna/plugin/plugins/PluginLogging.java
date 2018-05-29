package com.mewna.plugin.plugins;

import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.plugins.settings.LoggingSettings;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Logging", desc = "Log everything Mewna does in your server.", enabled = false, settings = LoggingSettings.class)
public class PluginLogging extends BasePlugin {
}
