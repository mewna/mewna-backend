package com.mewna.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.cache.entity.Guild;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.PlayerEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemUserEventType;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.*;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
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
@GIndex({"id", "guildXp", "ownedBackgroundPacks"})
@SuppressWarnings({"unused", "RedundantFieldInitialization"})
public class Player {
    @PrimaryKey
    private String id;
    private long balance = 0L;
    private long lastDaily = 0L;
    private long dailyStreak = 0L;
    private Map<String, Long> guildXp = new HashMap<>();
    private long globalXp = 0L;
    
    private Player(final String id) {
        this.id = id;
    }
    
    public static Player base(final String id) {
        return new Player(id);
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
        if(balance < 100_000L && balance + amount >= 100_000L) {
            Mewna.getInstance().getPluginManager().processEvent(EventType.PLAYER_EVENT,
                    new PlayerEvent(SystemUserEventType.MONEY, this,
                            new JSONObject().put("balance", 100_000L)));
        } if(balance < 1_000_000L && balance + amount >= 1_000_000L) {
            Mewna.getInstance().getPluginManager().processEvent(EventType.PLAYER_EVENT,
                    new PlayerEvent(SystemUserEventType.MONEY, this,
                            new JSONObject().put("balance", 1_000_000L)));
        }
        balance += amount;
        if(balance < 0L) {
            balance = 0L;
        }
    }
    
    // XP
    
    public long getXp(final String id) {
        if(guildXp.containsKey(id)) {
            return guildXp.get(id);
        } else {
            guildXp.put(id, 0L);
            return 0L;
        }
    }
    
    public long getXp(final Guild guild) {
        return getXp(guild.getId());
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
        final long avgXp = count == 0 ? 0 : guildXp / count;
        final long avg = balance + globalXp + dailyStreak + avgXp;
        return avg / 4;
    }
    
    // TODO: Caching
    // TODO: This is kinda dumb tbqh
    @JsonIgnore
    public Account getAccount() {
        //noinspection ConstantConditions
        return Mewna.getInstance().getDatabase().getAccountByDiscordId(id).get();
    }
}
