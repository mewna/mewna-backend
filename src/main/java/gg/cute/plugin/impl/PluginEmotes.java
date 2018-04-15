package gg.cute.plugin.impl;

import gg.cute.plugin.BasePlugin;
import gg.cute.plugin.Command;
import gg.cute.plugin.CommandContext;
import gg.cute.plugin.Plugin;

/**
 * @author amy
 * @since 4/8/18.
 */
@Plugin("emotes")
public class PluginEmotes extends BasePlugin {
    @Command(names = {"bap", "chew", "cookie", "hug", "lick", "nom", "poke", "prod", "shoot", "stab", "tickle"},
            desc = "Fun little emote commands. You need to mention someone to use them.", usage = "<command> <person>",
            examples = {"bap someone", "stab someone else", "poke everyone"}, aliased = false)
    public void emote(final CommandContext ctx) {
        final String action;
        switch(ctx.getCommand()) {
            case "bap":
                action = "bapped";
                break;
            case "chew":
                action = "chewed on";
                break;
            case "cookie":
                action = "given a :cookie:";
                break;
            case "hug":
                action = "hugged";
                break;
            case "lick":
                action = "licked";
                break;
            case "nom":
                action = "nommed on";
                break;
            case "poke":
                action = "poked";
                break;
            case "prod":
                action = "prodded";
                break;
            case "shoot":
                action = "shot :gun:";
                break;
            case "stab":
                action = "stabbed :knife:";
                break;
            case "tickle":
                action = "tickled";
                break;
            default:
                action = "hit by an unknown action";
                break;
        }
        getCute().getRestJDA().sendMessage(ctx.getChannel().getId(), String.format("%s, you were %s by %s!",
                ctx.getArgstr().replaceAll("@everyone", "very funny")
                        .replaceAll("@here", "nice try"), action, ctx.getUser().getName())).queue();
    }
}
