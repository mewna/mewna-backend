package com.mewna.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.ServerEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemEventType;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import io.vertx.core.json.JsonObject;
import lombok.*;

/**
 * @author amy
 * @since 4/15/19.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("servers")
@GIndex({"id", "description", "premium", "inBeta"})
public class Server {
    public static final int MAX_ABOUT_TEXT_LENGTH = 150;
    @JsonProperty("id")
    @PrimaryKey
    private String id;
    @JsonProperty("aboutText")
    private String aboutText = "A really cool server";
    @JsonProperty("customBackground")
    private String customBackground = "/backgrounds/default/plasma";
    @JsonProperty("premium")
    private boolean premium;
    @JsonProperty("inBeta")
    private boolean isInBeta;
    
    public Server(final String id) {
        this.id = id;
    }
    
    public boolean validate(final Server prev) {
        if(!premium && !Account.DEFAULT_BACKGROUNDS.contains(customBackground)) {
            return false;
        }
        return id != null
                && aboutText != null
                && !aboutText.isEmpty()
                && !aboutText.isBlank()
                && aboutText.length() <= MAX_ABOUT_TEXT_LENGTH
                && customBackground != null
                && !customBackground.isEmpty()
                && !customBackground.isBlank()
                && !(customBackground.startsWith("/") && customBackground.endsWith(".png"))
                && isInBeta == prev.isInBeta
                && premium == prev.premium
                ;
    }
    
    public void save(final Database database, final Server prev) {
        if(!prev.getAboutText().equalsIgnoreCase(getAboutText())) {
            Mewna.getInstance().pluginManager().processEvent(EventType.SERVER_EVENT,
                    new ServerEvent(SystemEventType.SERVER_DESCRIPTION, this,
                            new JsonObject()
                                    .put("old", prev.getAboutText())
                                    .put("new", getAboutText())));
        }
        if(!prev.getCustomBackground().equalsIgnoreCase(getCustomBackground())) {
            Mewna.getInstance().pluginManager().processEvent(EventType.SERVER_EVENT,
                    new ServerEvent(SystemEventType.SERVER_BACKGROUND, this,
                            new JsonObject().put("bg", getCustomBackground())));
        }
        database.saveServer(this);
    }
}
