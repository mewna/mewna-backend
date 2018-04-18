package gg.cute.data;

import gg.amy.pgorm.annotations.Index;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import gg.cute.cache.entity.Guild;
import gg.cute.plugin.CommandContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author amy
 * @since 4/10/18.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("players")
@Index("id")
@SuppressWarnings("unused")
public class Player {
    @PrimaryKey
    private String id;
    private Map<String, Long> guildBalances = new HashMap<>();
    private Map<String, Long> guildXp = new HashMap<>();
    private Map<String, Long> guildDailyTimes = new HashMap<>();
    private long globalXp;
    private long points;
    
    public static Player base(final String id) {
        return new Player(id, new HashMap<>(), new HashMap<>(), new HashMap<>(), 0L,0L);
    }
    
    // Daily
    
    public long getLastDaily(final Guild guild) {
        final String id = guild.getId();
        if(guildDailyTimes.containsKey(id)) {
            return guildDailyTimes.get(id);
        } else {
            guildDailyTimes.put(id, 0L);
            return 0L;
        }
    }
    
    public void updateLastDaily(final Guild guild) {
        final String id = guild.getId();
        guildDailyTimes.put(id, System.currentTimeMillis());
    }
    
    // Balance
    
    public long getBalance(final Guild guild) {
        final String id = guild.getId();
        if(guildBalances.containsKey(id)) {
            return guildBalances.get(id);
        } else {
            guildBalances.put(id, 0L);
            return 0L;
        }
    }
    
    public long getBalance(final CommandContext ctx) {
        return getBalance(ctx.getGuild());
    }
    
    public void incrementBalance(final Guild guild, final long amount) {
        incrementBalance(guild.getId(), amount);
    }
    
    public void incrementBalance(final String id, final long amount) {
        guildBalances.put(id, guildBalances.getOrDefault(id, 0L) + amount);
        if(guildBalances.get(id) < 0L) {
            guildBalances.put(id, 0L);
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
}
