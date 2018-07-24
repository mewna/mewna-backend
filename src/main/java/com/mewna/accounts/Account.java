package com.mewna.accounts;

import com.mewna.Mewna;
import com.mewna.data.Database;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.AccountEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemUserEventType;
import com.mewna.plugin.util.TextureManager;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.*;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author amy
 * @since 6/23/18.
 */
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Table("accounts")
@GIndex({"id", "email", "username", "discordAccountId"})
@Builder(toBuilder = true)
public class Account {
    public static final int MAX_ABOUT_TEXT_LENGTH = 150;
    public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Cras vehicula mi urna, nec tincidunt erat tincidunt eget. " +
            "Maecenas pretium consectetur metus.";
    
    @PrimaryKey
    private String id;
    private String email = "";
    private String username = "";
    private String displayName = "";
    private String discordAccountId = "";
    private String avatar = "";
    
    private String aboutText = "A mysterious stranger.";
    private String customBackground = "/backgrounds/default/plasma";
    private List<String> ownedBackgroundPacks = new ArrayList<>(Collections.singletonList("default"));
    
    @SuppressWarnings("WeakerAccess")
    public Account(final String id) {
        this.id = id;
    }
    
    // Configuration
    
    boolean validateSettings(final JSONObject data) {
        if(data.has("aboutText")) {
            final String aboutText = data.optString("aboutText");
            if(aboutText == null || aboutText.isEmpty()) {
                return false;
            }
            if(aboutText.length() > MAX_ABOUT_TEXT_LENGTH) {
                return false;
            }
        }
        if(data.has("customBackground")) {
            String bg = data.optString("customBackground");
            if(bg == null || bg.isEmpty()) {
                return false;
            }
            bg = bg.toLowerCase();
            if(bg.startsWith("/") || bg.endsWith("/") || bg.endsWith(".png")) {
                return false;
            }
            final String[] split = bg.split("/", 2);
            if(split.length != 2) {
                return false;
            }
            final String pack = split[0];
            final String name = split[1];
            if(!TextureManager.backgroundExists(pack, name)) {
                return false;
            }
            // I like this being explicit - it feels easier to reason about
            //noinspection RedundantIfStatement
            if(!ownedBackgroundPacks.contains(pack)) {
                return false;
            }
        }
        return true;
    }
    
    void updateSettings(final Database database, final JSONObject data) {
        final String id = data.getString("id");
        final AccountBuilder builder = database.getAccountById(id).map(Account::toBuilder).orElse(builder());
        int changes = 0;
        if(data.has("aboutText")) {
            if(!builder.aboutText.equals(data.getString("aboutText"))) {
                Mewna.getInstance().getPluginManager().processEvent(EventType.ACCOUNT_EVENT,
                        new AccountEvent(SystemUserEventType.DESCRIPTION, this,
                                new JSONObject()
                                        .put("old", builder.aboutText)
                                        .put("new", data.getString("aboutText"))
                        ));
            }
            builder.aboutText(data.getString("aboutText"));
            ++changes;
        }
        if(data.has("customBackground")) {
            if(!builder.customBackground.equals("/backgrounds/" + data.getString("customBackground"))) {
                Mewna.getInstance().getPluginManager().processEvent(EventType.ACCOUNT_EVENT,
                        new AccountEvent(SystemUserEventType.BACKGROUND, this,
                                new JSONObject().put("bg", "/backgrounds/" + data.getString("customBackground"))));
            }
            builder.customBackground("/backgrounds/" + data.getString("customBackground"));
            ++changes;
        }
        if(changes > 0) {
            database.saveAccount(builder.build());
        }
    }
}
