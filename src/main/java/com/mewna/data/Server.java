package com.mewna.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
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
@GIndex({"id", "description"})
public class Server {
    public static final int MAX_ABOUT_TEXT_LENGTH = 150;
    
    public Server(final String id) {
        this.id = id;
    }
    
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
    
    public boolean validate(final Server prev) {
        return id != null
                && aboutText != null
                && !aboutText.isEmpty()
                && !aboutText.isBlank()
                && aboutText.length() <= MAX_ABOUT_TEXT_LENGTH
                && customBackground != null
                && !customBackground.isEmpty()
                && !customBackground.isBlank()
                && isInBeta == prev.isInBeta
                && premium == prev.premium
                ;
    }
}
