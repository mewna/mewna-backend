package com.mewna.plugin.plugins;

import com.mewna.accounts.Account;
import com.mewna.accounts.timeline.TimelinePost;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.UserEvent;
import com.mewna.plugin.plugins.settings.BehaviourSettings;

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
}
