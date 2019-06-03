package com.mewna.plugin.plugins;

import com.mewna.data.accounts.Account;
import com.mewna.data.accounts.timeline.TimelinePost;
import com.mewna.plugin.BasePlugin;
import com.mewna.plugin.Plugin;
import com.mewna.plugin.event.Event;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.AccountEvent;
import com.mewna.plugin.event.plugin.behaviour.PlayerEvent;
import com.mewna.plugin.event.plugin.behaviour.ServerEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemEventType;
import com.mewna.plugin.plugins.settings.BehaviourSettings;
import io.vertx.core.json.JsonObject;

/**
 * @author amy
 * @since 6/23/18.
 */
@Plugin(name = "Behaviour", desc = "Change how Mewna behaves in this server.", settings = BehaviourSettings.class)
public class PluginBehaviour extends BasePlugin {
    @Event(EventType.PLAYER_EVENT)
    public void handlePlayerEvent(final PlayerEvent event) {
        database().getAccountByDiscordId(event.getPlayer().getId())
                .ifPresent(acc -> handleUserEvent(acc, event.getType(), event.getData()));
    }
    
    @Event(EventType.ACCOUNT_EVENT)
    public void handleAccountEvent(final AccountEvent event) {
        handleUserEvent(event.getAccount(), event.getType(), event.getData());
    }
    
    @Event(EventType.SERVER_EVENT)
    public void handleServerEvent(final ServerEvent event) {
        final var id = event.getServer().getId();
        final var type = event.getType();
        final var data = event.getData();
        final TimelinePost post = TimelinePost.create(id, true, data
                .put("type", type.getEventId()).toString());
        database().saveTimelinePost(post);
    }
    
    private void handleUserEvent(final Account account, final SystemEventType type, final JsonObject data) {
        final TimelinePost post = TimelinePost.create(account.id(), true, data
                .put("type", type.getEventId()).toString());
        database().saveTimelinePost(post);
    }
}
