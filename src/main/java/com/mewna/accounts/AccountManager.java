package com.mewna.accounts;

import com.mewna.Mewna;
import com.mewna.accounts.Account.AccountBuilder;
import com.mewna.accounts.timeline.TimelinePost;
import com.mewna.catnip.entity.user.User;
import com.mewna.catnip.entity.util.ImageOptions;
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
@SuppressWarnings("unused")
@RequiredArgsConstructor
public class AccountManager {
    private final Mewna mewna;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    public Optional<Account> getAccountById(final String id) {
        return mewna.database().getAccountById(id);
    }
    
    public Optional<Account> getAccountByLinkedDiscord(final String discordId) {
        return mewna.database().getAccountByDiscordId(discordId);
    }
    
    public String checkDiscordLinkedAccountExists(final String id) {
        final Optional<Account> account = getAccountByLinkedDiscord(id);
        return account.map(Account::id).orElse("null");
    }
    
    public void createNewDiscordLinkedAccount(final Player player, final User user) {
        final String snowflake = user.id();
        final Account account = new Account(snowflake);
        
        account.discordAccountId(user.id());
        account.displayName(user.username());
        account.avatar(user.effectiveAvatarUrl(new ImageOptions().png()));
        
        mewna.database().saveAccount(account);
    }
    
    public void createOrUpdateDiscordOAuthLinkedAccount(final JsonObject data) {
        final boolean isNew = !data.containsKey("id") || !data.getString("id").matches("\\d+");
        final String id = isNew
                ? Snowflakes.getNewSnowflake()
                : data.getString("id");
        final AccountBuilder builder = mewna.database().getAccountById(id).map(Account::toBuilder)
                // We do this to get the default values so that we don't have to do extra work here later.
                .orElseGet(() -> new Account(id).toBuilder());
        if(data.containsKey("discordAccountId") && data.getString("discordAccountId", null) != null) {
            final String discordAccountId = data.getString("discordAccountId", null);
            if(discordAccountId != null && !discordAccountId.isEmpty()) {
                builder.id(discordAccountId);
                builder.discordAccountId(discordAccountId);
            } else {
                return;
            }
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
            if(isNew && data.containsKey("displayName") && data.getString("displayName", null) != null) {
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
            mewna.database().saveAccount(builder.build());
            if(isNew) {
                logger.info("Created account {}", discordAccountId);
            } else {
                logger.info("Updated account {}", discordAccountId);
            }
        }
    }
    
    public void updateAccountSettings(final JsonObject data) {
        final String id = data.getString("id", null);
        if(id != null && !id.isEmpty()) {
            final Optional<Account> maybeAccount = mewna.database().getAccountById(id);
            if(maybeAccount.isPresent()) {
                final Account account = maybeAccount.get();
                if(account.validateSettings(data)) {
                    account.updateSettings(mewna.database(), id, data);
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
        return mewna.database().getAllTimelinePosts(id);
    }
}
