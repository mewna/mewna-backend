package com.mewna.plugin.plugins;

import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.commands.Command;
import com.mewna.plugin.commands.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.commands.annotations.Multi;
import com.mewna.plugin.plugins.settings.EmotesSettings;

import static com.mewna.util.Translator.$;

/**
 * @author amy
 * @since 5/19/18.
 */
@Plugin(name = "Emotes", desc = "Fun little emotes for your server.", settings = EmotesSettings.class)
public class PluginEmotes extends BasePlugin {
    @Multi
    @Command(names = {"bap", "chew", "cookie", "hug", "lick", "nom",
            "pat", "poke", "prod", "shoot", "slap", "stab", "tickle"},
            desc = "commands.emotes", usage = "<command> <person>",
            examples = {"bap someone", "stab someone else", "poke everyone"})
    public void emote(final CommandContext ctx) {
        if(ctx.getArgstr().isEmpty()) {
            ctx.sendMessage("You need to tell me who you're doing that to.");
            return;
        }
        
        final String action = $(ctx.getLanguage(), "plugins.emotes.commands." + ctx.getCommand());
        final String base = $(ctx.getLanguage(), "plugins.emotes.base");
        final String out = base
                .replace("$target", ctx.getArgstr())
                .replace("$user", ctx.getUser().username())
                .replace("$action", action)
                .replace("@everyone", "very funny")
                .replace("@here", "very funny");
        
        ctx.sendMessage(out);
    }
}
