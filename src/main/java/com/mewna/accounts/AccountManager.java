package com.mewna.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.Mewna;
import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    private final OkHttpClient client = new OkHttpClient.Builder().build();
    
    public Optional<Account> getAccountById(final String id) {
        return mewna.getDatabase().getAccountById(id);
    }
    
    public Optional<Account> getAccountByLinkedDiscord(final String discordId) {
        return mewna.getDatabase().getAccountByDiscordId(discordId);
    }
    
    public String checkDiscordLinkedAccountExists(final String id) {
        final Optional<Account> account = getAccountByLinkedDiscord(id);
        if(account.isPresent()) {
            return account.get().getId();
        } else {
            return id;
        }
    }
    
    public void createOrUpdateUser(final String json) {
        final JSONObject data = new JSONObject(json);
        if(!data.has("id")) {
            data.put("id", getNewSnowflake());
        }
        
        try {
            final Account account = MAPPER.readValue(data.toString(), Account.class);
            mewna.getDatabase().saveAccount(account);
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @SuppressWarnings("UnnecessarilyQualifiedInnerClassAccess")
    public String getNewSnowflake() {
        try {
            @SuppressWarnings("ConstantConditions")
            final String snowflake = client.newCall(new Request.Builder().build()).execute().body().string();
            return snowflake;
        } catch(final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
