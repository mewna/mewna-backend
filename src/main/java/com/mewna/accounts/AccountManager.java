package com.mewna.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mewna.Mewna;
import com.mewna.accounts.Account.AccountBuilder;
import com.mewna.accounts.timeline.TimelinePost;
import com.mewna.cache.entity.User;
import com.mewna.data.Player;
import com.mewna.plugin.util.Snowflakes;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
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
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
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
    
    public void createNewDiscordLinkedAccount(final Player player, final User user) {
        final String snowflake = Snowflakes.getNewSnowflake();
        final Account account = new Account(snowflake);
        
        account.setDiscordAccountId(user.getId());
        account.setDisplayName(user.getName());
        account.setAvatar(user.getAvatarURL());
        
        mewna.getDatabase().saveAccount(account);
    }
    
    public void createOrUpdateDiscordOAuthLinkedAccount(final JsonObject data) {
        final String id = data.containsKey("id") && data.getString("id", "").matches("\\d+")
                ? data.getString("id")
                : Snowflakes.getNewSnowflake();
        final AccountBuilder builder = mewna.getDatabase().getAccountById(id).map(Account::toBuilder)
                // We do this to get the default values so that we don't have to do extra work here later.
                .orElseGet(() -> new Account(id).toBuilder());
        builder.id(id);
        
        if(data.containsKey("username") && data.getString("username", null) != null) {
            final String username = data.getString("username", null);
            if(username != null && !username.isEmpty()) {
                builder.username(username);
            }
        }
        if(data.containsKey("email") && data.getString("email", null) != null) {
            final String email = data.getString("email", null);
            if(email != null && !email.isEmpty()) {
                builder.email(email);
            }
        }
        if(data.containsKey("displayName") && data.getString("displayName", null) != null) {
            final String displayName = data.getString("displayName", null);
            if(displayName != null && !displayName.isEmpty()) {
                builder.displayName(displayName);
            }
        }
        if(data.containsKey("avatar") && data.getString("avatar", null) != null) {
            final String avatar = data.getString("avatar", null);
            if(avatar != null && !avatar.isEmpty()) {
                builder.avatar(avatar);
            }
        }
        if(data.containsKey("discordAccountId") && data.getString("discordAccountId", null) != null) {
            final String discordAccountId = data.getString("discordAccountId", null);
            if(discordAccountId != null && !discordAccountId.isEmpty()) {
                builder.discordAccountId(discordAccountId);
            }
        }
        mewna.getDatabase().saveAccount(builder.build());
    }
    
    public void updateAccountSettings(final JsonObject data) {
        final String id = data.getString("id", null);
        if(id != null && !id.isEmpty()) {
            final Optional<Account> maybeAccount = mewna.getDatabase().getAccountById(id);
            if(maybeAccount.isPresent()) {
                final Account account = maybeAccount.get();
                if(account.validateSettings(data)) {
                    account.updateSettings(mewna.getDatabase(), data);
                    logger.info("Updated account {}", id);
                } else {
                    logger.warn("Can't update account {}: Failed validateSettings, data {}", id, data);
                }
            } else {
                logger.warn("Can't update account {}: Doesn't exist", id);
            }
        } else {
            logger.warn("No id with account payload: {}", data);
        }
    }
    
    public List<TimelinePost> getAllPosts(final String id) {
        return mewna.getDatabase().getAllTimelinePosts(id);
    }
}
