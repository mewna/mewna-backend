package com.mewna.plugin.plugins;

import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.plugins.settings.TwitchSettings;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Twitch", desc = "Get alerts when your favourite streamers go live.", settings = TwitchSettings.class)
public class PluginTwitch extends BasePlugin {
}
