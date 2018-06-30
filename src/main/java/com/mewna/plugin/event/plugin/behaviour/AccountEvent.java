package com.mewna.plugin.event.plugin.behaviour;

import com.mewna.accounts.Account;
import com.mewna.plugin.event.BaseEvent;
import com.mewna.plugin.event.EventType;
import lombok.Getter;
import org.json.JSONObject;

/**
 * @author amy
 * @since 6/30/18.
 */
@Getter
public class AccountEvent extends BaseEvent {
    private final SystemUserEventType type;
    private final Account account;
    private final JSONObject data;
    
    public AccountEvent(final SystemUserEventType type, final Account account, final JSONObject data) {
        super(EventType.ACCOUNT_EVENT);
        this.type = type;
        this.account = account;
        this.data = data;
    }
}
