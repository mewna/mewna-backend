package com.mewna.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.Mewna;
import com.mewna.accounts.Account.AccountBuilder;
import com.mewna.plugin.util.Snowflakes;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Optional;

/**
 * @author amy
 * @since 6/24/18.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@RequiredArgsConstructor
public class AccountManager {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Mewna mewna;
    
    public Optional<Account> getAccountById(final String id) {
        return mewna.getDatabase().getAccountById(id);
    }
    
    public Optional<Account> getAccountByLinkedDiscord(final String discordId) {
        return mewna.getDatabase().getAccountByDiscordId(discordId);
    }
    
    public String checkDiscordLinkedAccountExists(final String id) {
        final Optional<Account> account = getAccountByLinkedDiscord(id);
        return account.map(Account::getId).orElse("null");
    }
    
    public void createOrUpdateUser(final String json) {
        final JSONObject data = new JSONObject(json);
        if(!data.has("id")) {
            data.put("id", Snowflakes.getNewSnowflake());
        }
        
        try {
            final Optional<Account> maybeAccount = mewna.getDatabase().getAccountById(data.getString("id"));
            final Account account = MAPPER.readValue(data.toString(), Account.class);
            if(!maybeAccount.isPresent()) {
                // No existing account, just update directly
                mewna.getDatabase().saveAccount(account);
            } else {
                // It claims there's no check for presence before .get(), but it's literally right above this
                //noinspection ConstantConditions
                final AccountBuilder builder = maybeAccount.map(Account::toBuilder).get();
                // Existing account, merge
                if(data.has("email")) {
                    final String email = data.optString("email");
                    if(email != null && !email.trim().isEmpty()) {
                        builder.email(email);
                    }
                }
                if(data.has("username")) {
                    final String username = data.optString("username");
                    if(username != null && !username.trim().isEmpty()) {
                        builder.username(username);
                    }
                }
                if(data.has("displayName")) {
                    final String displayName = data.optString("displayName");
                    if(displayName != null && !displayName.trim().isEmpty()) {
                        builder.displayName(displayName);
                    }
                }
                if(data.has("discordAccountId")) {
                    final String discordAccountId = data.optString("discordAccountId");
                    if(discordAccountId != null && !discordAccountId.trim().isEmpty()) {
                        builder.discordAccountId(discordAccountId);
                    }
                }
                if(data.has("avatar")) {
                    final String avatar = data.optString("avatar");
                    if(avatar != null && !avatar.trim().isEmpty()) {
                        builder.avatar(avatar);
                    }
                }
                mewna.getDatabase().saveAccount(builder.build());
            }
            
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
