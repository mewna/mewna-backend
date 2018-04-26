package gg.cute.data;

import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Value;

/**
 * @author amy
 * @since 4/10/18.
 */
@Value
@Table("discord_settings")
@Index("id")
public class DiscordSettings {
    @PrimaryKey
    private String id;
    private String customPrefix;
    private String currencySymbol;
    private boolean tipsEnabled;
    private boolean radioEnabled;
    
    public static DiscordSettings base(final String id) {
        return new DiscordSettings(id, null, null, true, true);
    }
}
