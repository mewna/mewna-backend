package com.mewna.plugin.plugins;

import com.mewna.accounts.Account;
import com.mewna.accounts.timeline.TimelinePost;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Command;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.UserEvent;
import com.mewna.plugin.event.plugin.behaviour.UserEvent.UserEventType;
import com.mewna.plugin.plugins.settings.BehaviourSettings;
import org.json.JSONObject;

import java.util.Optional;

/**
 * @author amy
 * @since 6/23/18.
 */
@Plugin(name = "Behaviour", desc = "Change how Mewna behaves in this server.", settings = BehaviourSettings.class)
public class PluginBehaviour extends BasePlugin {
    @Event(EventType.USER_EVENT)
    public void handleEvent(final UserEvent event) {
        final String playerId = event.getPlayer().getId();
        final Optional<Account> account = getDatabase().getAccountByDiscordId(playerId);
        final String id = account.map(Account::getId).orElse(playerId);
        final TimelinePost post = TimelinePost.create(id, true, event.getData()
                .put("type", event.getType().getEventId()).toString());
        getDatabase().savePost(post);
        switch(event.getType()) {
            case GLOBAL_LEVEL: {
                // TODO: Announce in Discord?
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
            default: {
                getLogger().warn("Got unknown UET: " + event.getType().name());
                break;
            }
        }
    }
    
    // TODO: I just wanna highlight this so it's obvious
    // The data show in here is the data expected to be sent in the events.
    // When creating the events for these, we should pay extra attention
    // to these.
    @Command(names = "postme", desc = "dot", usage = {}, examples = {})
    public void postMe(final CommandContext ctx) throws InterruptedException {
        final String playerId = ctx.getPlayer().getId();
        final Optional<Account> account = getDatabase().getAccountByDiscordId(playerId);
        final String id = account.map(Account::getId).orElse(playerId);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("bg", "/backgrounds/default/plasma")
                    .put("type", UserEventType.BACKGROUND.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getRestJDA().sendMessage(ctx.getChannel(), "Posting 1/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("descOld", "A mysterious stranger")
                    .put("descNew", "A mysterious stranger")
                    .put("type", UserEventType.DESCRIPTION.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getRestJDA().sendMessage(ctx.getChannel(), "Posting 2/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("level", "10")
                    .put("type", UserEventType.GLOBAL_LEVEL.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getRestJDA().sendMessage(ctx.getChannel(), "Posting 3/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("balance", "100000")
                    .put("type", UserEventType.MONEY.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getRestJDA().sendMessage(ctx.getChannel(), "Posting 4/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("streamMode", "start")
                    .put("streamTitle", "meme test stream")
                    .put("type", UserEventType.TWITCH_STREAM.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getRestJDA().sendMessage(ctx.getChannel(), "Posting 5/6").queue();
        Thread.sleep(60_000);
        {
            final TimelinePost post = TimelinePost.create(id, true, new JSONObject()
                    .put("streamMode", "end")
                    .put("streamTitle", "meme test stream")
                    .put("type", UserEventType.TWITCH_STREAM.getEventId()).toString());
            getDatabase().savePost(post);
        }
        getRestJDA().sendMessage(ctx.getChannel(), "Posting 6/6").queue();
    }
}
