package com.mewna.data.accounts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mewna.Mewna;
import com.mewna.data.Database;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.AccountEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemEventType;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import io.vertx.core.json.JsonObject;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author amy
 * @since 6/23/18.
 */
@Getter
@Setter
@Accessors(fluent = true)
@AllArgsConstructor
@RequiredArgsConstructor
@Table("accounts")
@GIndex({"id", "email", "username", "discordAccountId"})
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class Account {
    public static final List<String> DEFAULT_BACKGROUNDS = List.of("/backgrounds/default/plasma",
            "/backgrounds/default/rainbow_triangles",
            "/backgrounds/default/triangles");
    
    public static final int MAX_ABOUT_TEXT_LENGTH = 150;
    public static final int MAX_DISPLAY_NAME_LENGTH = 32;
    public static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Cras vehicula mi urna, nec tincidunt erat tincidunt eget. " +
            "Maecenas pretium consectetur metus.";
    
    @PrimaryKey
    @JsonProperty("id")
    private String id;
    @JsonProperty("email")
    private String email = "";
    @JsonProperty("username")
    private String username = "";
    @JsonProperty("displayName")
    private String displayName = "";
    @JsonProperty("discordAccountId")
    private String discordAccountId = "";
    @JsonProperty("avatar")
    private String avatar = "";
    
    @JsonProperty("aboutText")
    private String aboutText = "A mysterious stranger.";
    @JsonProperty("customBackground")
    private String customBackground = "/backgrounds/default/plasma";
    @Deprecated
    @JsonProperty("ownedBackgroundPacks")
    private List<String> ownedBackgroundPacks = new ArrayList<>(Collections.singletonList("default"));
    @JsonProperty("inBeta")
    private boolean isInBeta;
    @JsonProperty("banned")
    private boolean banned;
    @Deprecated
    @JsonProperty("isBanned")
    private boolean isBanned;
    @JsonProperty("banReason")
    private String banReason;
    @JsonProperty("premium")
    private boolean premium;
    
    public Account(final String id) {
        this.id = id;
    }
    
    // Configuration
    
    boolean validateSettings(final JsonObject data) {
        if(data.containsKey("aboutText")) {
            final String aboutText = data.getString("aboutText", null);
            if(aboutText == null || aboutText.isEmpty()) {
                return false;
            }
            if(aboutText.length() > MAX_ABOUT_TEXT_LENGTH) {
                return false;
            }
        }
        if(data.containsKey("displayName")) {
            final String displayName = data.getString("displayName", null);
            if(displayName == null || displayName.isEmpty()) {
                return false;
            }
            if(displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
                return false;
            }
        }
        if(data.containsKey("customBackground")) {
            String bg = data.getString("customBackground", null);
            if(bg == null || bg.isEmpty()) {
                return false;
            }
            bg = bg.toLowerCase();
            // I like this being explicit; it's easier to reason about imo + it
            // leaves room for future expansions
            //noinspection RedundantIfStatement
            if(!premium && !DEFAULT_BACKGROUNDS.contains(bg)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean updateSettings(final Database database, final String id, final JsonObject data) {
        if(!validateSettings(data)) {
            return false;
        }
        int changes = 0;
        if(data.containsKey("aboutText")) {
            if(!aboutText().equals(data.getString("aboutText"))) {
                Mewna.getInstance().pluginManager().processEvent(EventType.ACCOUNT_EVENT,
                        new AccountEvent(SystemEventType.ACCOUNT_DESCRIPTION, this,
                                new JsonObject()
                                        .put("old", aboutText())
                                        .put("new", data.getString("aboutText"))
                        ));
            }
            aboutText(data.getString("aboutText"));
            ++changes;
        }
        if(data.containsKey("customBackground")) {
            if(!customBackground().equals(data.getString("customBackground"))) {
                Mewna.getInstance().pluginManager().processEvent(EventType.ACCOUNT_EVENT,
                        new AccountEvent(SystemEventType.ACCOUNT_BACKGROUND, this,
                                new JsonObject().put("bg", data.getString("customBackground"))));
            }
            customBackground(data.getString("customBackground"));
            ++changes;
        }
        if(data.containsKey("displayName")) {
            if(!displayName().equals(data.getString("displayName"))) {
                Mewna.getInstance().pluginManager().processEvent(EventType.ACCOUNT_EVENT,
                        new AccountEvent(SystemEventType.ACCOUNT_DISPLAY_NAME, this,
                                new JsonObject()
                                        .put("old", displayName())
                                        .put("new", data.getString("displayName"))
                        ));
            }
            displayName(data.getString("displayName"));
            ++changes;
        }
        if(changes > 0) {
            database.saveAccount(this);
        }
        return true;
    }
}
