package com.mewna.plugin.plugins;

import com.mewna.accounts.Account;
import com.mewna.accounts.timeline.TimelinePost;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.AccountEvent;
import com.mewna.plugin.event.plugin.behaviour.PlayerEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemUserEventType;
import com.mewna.plugin.plugins.settings.BehaviourSettings;
import io.vertx.core.json.JsonObject;

/**
 * @author amy
 * @since 6/23/18.
 */
@Plugin(name = "Behaviour", desc = "Change how Mewna behaves in this server.", settings = BehaviourSettings.class)
public class PluginBehaviour extends BasePlugin {
    @Event(EventType.PLAYER_EVENT)
    public void handleEvent(final PlayerEvent event) {
        getDatabase().getAccountByDiscordId(event.getPlayer().getId())
                .ifPresent(acc -> handle(acc, event.getType(), event.getData()));
    }
    
    @Event(EventType.ACCOUNT_EVENT)
    public void handleEvent(final AccountEvent event) {
        handle(event.getAccount(), event.getType(), event.getData());
    }
    
    private void handle(final Account account, final SystemUserEventType type, final JsonObject data) {
        final TimelinePost post = TimelinePost.create(account.getId(), true, data
                .put("type", type.getEventId()).toString());
        getDatabase().savePost(post);
        // TODO: Announce in Discord or what? :V
        switch(type) {
            case GLOBAL_LEVEL: {
                break;
            }
            case BACKGROUND: {
                break;
            }
            case DESCRIPTION: {
                break;
            }
            case TWITCH_STREAM: {
                break;
            }
            case MONEY: {
                break;
            }
            default: {
                getLogger().warn("Got unknown SUET: " + type.name());
                break;
            }
        }
    }
    
    // TODO: I just wanna highlight this so it's obvious
    // The data show in here is the data expected to be sent in the events.
    // When creating the events for these, we should pay extra attention
    // to these.
    /*
    @Command(names = "postme", desc = "dot", usage = {}, examples = {})
    public void postMe(final CommandContext ctx) throws InterruptedException {
        final String playerId = ctx.getPlayer().getId();
        final Optional<Account> account = getDatabase().getAccountByDiscordId(playerId);
        final String id = account.map(Account::getId).orElse(playerId);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("bg", "/backgrounds/default/plasma")
                    .put("type", BACKGROUND.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getCatnip().sendMessage(ctx.getChannel(), "Posting 1/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("descOld", "A mysterious stranger")
                    .put("descNew", "A mysterious stranger")
                    .put("type", DESCRIPTION.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getCatnip().sendMessage(ctx.getChannel(), "Posting 2/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("level", "10")
                    .put("type", GLOBAL_LEVEL.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getCatnip().sendMessage(ctx.getChannel(), "Posting 3/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("balance", "100000")
                    .put("type", MONEY.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getCatnip().sendMessage(ctx.getChannel(), "Posting 4/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("streamMode", "start")
                    .put("streamTitle", "meme test stream")
                    .put("type", TWITCH_STREAM.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getCatnip().sendMessage(ctx.getChannel(), "Posting 5/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("streamMode", "end")
                    .put("streamTitle", "meme test stream")
                    .put("type", TWITCH_STREAM.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getCatnip().sendMessage(ctx.getChannel(), "Posting 6/6").queue();
    }
    */
}
