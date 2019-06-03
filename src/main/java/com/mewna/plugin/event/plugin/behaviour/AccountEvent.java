package com.mewna.plugin.event.plugin.behaviour;

import com.mewna.data.accounts.Account;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import io.vertx.core.json.JsonObject;
import lombok.Getter;

/**
 * @author amy
 * @since 6/30/18.
 */
@Getter
public class AccountEvent extends BaseEvent {
    private final SystemEventType type;
    private final Account account;
    private final JsonObject data;
    
    public AccountEvent(final SystemEventType type, final Account account, final JsonObject data) {
        super(EventType.ACCOUNT_EVENT);
        this.type = type;
        this.account = account;
        this.data = data;
    }
}
