package com.mewna.plugin.plugins;

import com.mewna.accounts.Account;
import com.mewna.data.Player;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.plugins.settings.SecretSettings;
import com.mewna.plugin.util.Emotes;
import org.json.JSONObject;

import java.util.Optional;

/**
 * @author amy
 * @since 10/18/18.
 */
@Plugin(name = "secret", desc = "spooky secret things :3", settings = SecretSettings.class, owner = true)
public class PluginSecret extends BasePlugin {
    @Command(names = "secret", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void secret(final CommandContext ctx) {
        getRestJDA().sendMessage(ctx.getChannel(), "secret").queue();
    }
    
    @Command(names = "inspect", desc = "secret", usage = "secret", examples = "secret", owner = true)
    public void debugInspect(final CommandContext ctx) {
        if(ctx.getArgs().size() != 2) {
            getRestJDA().sendMessage(ctx.getChannel(), Emotes.NO).queue();
        } else {
            final String snowflake = ctx.getArgs().get(1)
                    .replaceAll("<@(!)?", "")
                    .replace(">", "");
            switch(ctx.getArgs().get(0).toLowerCase()) {
                case "player": {
                    final Optional<Player> optionalPlayer = getDatabase().getOptionalPlayer(snowflake);
                    if(optionalPlayer.isPresent()) {
                        final JSONObject o = new JSONObject(optionalPlayer.get());
                        // TODO: ???
                        o.remove("account");
                        o.remove("votes");
                        o.remove("boxes");
                        final String json = o.toString(2);
                        getRestJDA().sendMessage(ctx.getChannel(), "```Javascript\n"+json+"\n```").queue();
                    } else {
                        getRestJDA().sendMessage(ctx.getChannel(), Emotes.NO).queue();
                    }
                    break;
                }
                case "account": {
                    final Optional<Account> optionalAccount = getMewna().getAccountManager().getAccountByLinkedDiscord(snowflake);
                    if(optionalAccount.isPresent()) {
                        final JSONObject o = new JSONObject(optionalAccount.get());
                        o.remove("email");
                        final String json = o.toString(2);
                        getRestJDA().sendMessage(ctx.getChannel(), "```Javascript\n"+json+"\n```").queue();
                    } else {
                        getRestJDA().sendMessage(ctx.getChannel(), Emotes.NO).queue();
                    }
                    break;
                }
                default: {
                    getRestJDA().sendMessage(ctx.getChannel(), Emotes.NO).queue();
                    break;
                }
            }
        }
    }
}
