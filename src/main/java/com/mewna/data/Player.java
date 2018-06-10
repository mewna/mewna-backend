package com.mewna.data;

import com.mewna.cache.entity.Guild;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.util.TextureManager;
import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.*;
import org.json.JSONObject;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author amy
 * @since 4/10/18.
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("players")
@Index({"id", "guildXp", "guildBalances"})
@SuppressWarnings("unused")
public class Player {
    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
            "Cras vehicula mi urna, nec tincidunt erat tincidunt eget. " +
            "Maecenas pretium consectetur metus.";
    @PrimaryKey
    private String id;
    private long balance;
    private long lastDaily;
    private long dailyStreak;
    private Map<String, Long> guildXp = new HashMap<>();
    private long globalXp;
    private long points;
    private String aboutText;
    private String customBackground;
    private List<String> ownedBackgroundPacks;
    
    public static final int MAX_ABOUT_TEXT_LENGTH = 150;
    
    public static Player base(final String id) {
        return new Player(id, 0L, 0L, 0L, new HashMap<>(), 0L, 0L,
                /*"A mysterious stranger."*/ LOREM_IPSUM, "/backgrounds/default/plasma",
                new ArrayList<>(Collections.singletonList("default")));
    }
    
    // Configuration
    
    public boolean validateSettings(final JSONObject data) {
        if(data.has("id") || data.has("balance") || data.has("lastDaily") || data.has("dailyStreak")
                || data.has("guildXp") || data.has("globalXp") || data.has("points")) {
            return false;
        } else {
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
                // I like this being explicit. I find it easier to reason about.
                //noinspection RedundantIfStatement
                if(!TextureManager.backgroundExists(pack, name)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    public void updateSettings(final Database database, final JSONObject data) {
        final PlayerBuilder builder = toBuilder();
        int changes = 0;
        if(data.has("aboutText")) {
            builder.aboutText(data.getString("aboutText"));
            ++changes;
        }
        if(data.has("customBackground")) {
            builder.customBackground("/backgrounds/" + data.getString("customBackground"));
            ++changes;
        }
        if(changes > 0) {
            database.savePlayer(builder.build());
        }
    }
    
    // Daily
    
    public void updateLastDaily() {
        lastDaily = System.currentTimeMillis();
    }
    
    public void incrementDailyStreak() {
        dailyStreak += 1;
    }
    
    public void resetDailyStreak() {
        dailyStreak = 0;
    }
    
    // Balance
    
    public void incrementBalance(final long amount) {
        balance += amount;
        if(balance < 0L) {
            balance = 0L;
        }
    }
    
    // XP
    
    public long getXp(final Guild guild) {
        final String id = guild.getId();
        if(guildXp.containsKey(id)) {
            return guildXp.get(id);
        } else {
            guildXp.put(id, 0L);
            return 0L;
        }
    }
    
    public long getXp(final CommandContext ctx) {
        return getXp(ctx.getGuild());
    }
    
    @SuppressWarnings("WeakerAccess")
    public void incrementLocalXp(final String id, final long amount) {
        guildXp.put(id, guildXp.getOrDefault(id, 0L) + amount);
        if(guildXp.get(id) < 0L) {
            guildXp.put(id, 0L);
        }
    }
    
    public void incrementLocalXp(final Guild guild, final long amount) {
        incrementLocalXp(guild.getId(), amount);
    }
    
    public void incrementGlobalXp(final long amount) {
        globalXp += amount;
    }
    
    // Scoring
    
    public long calculateScore() {
        int count = 0;
        int guildXp = 0;
        for(final Entry<String, Long> entry : this.guildXp.entrySet()) {
            guildXp += entry.getValue();
            ++count;
        }
        //noinspection UnnecessaryParentheses
        final long avg = balance + points + globalXp + dailyStreak + (guildXp / count);
        return avg / 5;
    }
}
