package com.mewna.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.cache.entity.Guild;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.event.EventType;
import com.mewna.plugin.event.plugin.behaviour.PlayerEvent;
import com.mewna.plugin.event.plugin.behaviour.SystemUserEventType;
import com.mewna.plugin.plugins.economy.Box;
import com.mewna.plugin.plugins.economy.Item;
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
@SuppressWarnings({"unused", "RedundantFieldInitialization", "WeakerAccess", "UnusedReturnValue"})
public class Player {
    public static final long MAX_INV_WEIGHT = 10_000;
    
    @PrimaryKey
    private String id;
    private long balance = 0L;
    private long lastDaily = 0L;
    private long dailyStreak = 0L;
    private Map<String, Long> guildXp = new HashMap<>();
    private long globalXp = 0L;
    private Map<Box, Long> boxes = new HashMap<>();
    private Map<Item, Long> items = new HashMap<>();
    
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
        }
        if(balance < 1_000_000L && balance + amount >= 1_000_000L) {
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
    
    // Items
    
    public long calculateInventoryWeight() {
        final long[] w = {0};
        if(items != null) {
            items.forEach((k, v) -> w[0] += k.getWeight() * v);
        }
        return w[0];
    }
    
    public Player addOneToInventory(final Item item) {
        if(items == null) {
            items = new HashMap<>();
        }
        items.putIfAbsent(item, 0L);
        items.put(item, items.get(item) + 1);
        
        return this;
    }
    
    public Player addToInventory(final Iterable<Item> input) {
        input.forEach(this::addOneToInventory);
        return this;
    }
    
    public Player addAllToInventory(final Map<Item, Long> input) {
        if(items == null) {
            items = new HashMap<>();
        }
        input.forEach((i, c) -> {
            items.putIfAbsent(i, 0L);
            items.put(i, items.get(i) + c);
        });
        
        return this;
    }
    
    public Player removeOneFromInventory(final Item item) {
        if(items == null) {
            items = new HashMap<>();
        }
        if(items.containsKey(item)) {
            items.put(item, items.get(item) - 1);
        }
        
        return this;
    }
    
    public Player removeAllFromInventory(final Map<Item, Long> input) {
        if(items == null) {
            items = new HashMap<>();
        }
        input.forEach((item, count) -> {
            if(items.containsKey(item)) {
                items.put(item, items.get(item) - 1);
            }
        });
        
        return this;
    }
    
    public boolean hasItem(final Item item) {
        return items.containsKey(item) && items.get(item) > 0L;
    }
    
    // Boxes
    
    public Player addOneToBoxes(final Box boxen) {
        if(boxes == null) {
            boxes = new HashMap<>();
        }
        boxes.putIfAbsent(boxen, 0L);
        boxes.put(boxen, boxes.get(boxen) + 1);
        
        return this;
    }
    
    public Player addToBoxes(final Iterable<Box> boxen) {
        boxen.forEach(this::addOneToBoxes);
        return this;
    }
    
    public Player addAllToBoxes(final Map<Box, Long> boxen) {
        if(boxes == null) {
            boxes = new HashMap<>();
        }
        boxen.forEach((i, c) -> {
            boxes.putIfAbsent(i, 0L);
            boxes.put(i, boxes.get(i) + c);
        });
        
        return this;
    }
    
    public Player removeOneFromBoxes(final Box boxen) {
        if(boxes == null) {
            boxes = new HashMap<>();
        }
        if(boxes.containsKey(boxen)) {
            boxes.put(boxen, boxes.get(boxen) - 1);
        }
        
        return this;
    }
    
    public Player removeAllFromBoxes(final Map<Box, Long> boxen) {
        if(boxes == null) {
            boxes = new HashMap<>();
        }
        boxen.forEach((item, count) -> {
            if(boxes.containsKey(item)) {
                boxes.put(item, boxes.get(item) - 1);
            }
        });
        
        return this;
    }
    
    /**
     * Clean up the player's data so that it's "safe" to put in the DB
     */
    public void cleanup() {
        if(items != null) {
            items.values().removeIf(l -> l == 0L);
        }
        if(boxes != null) {
            boxes.values().removeIf(l -> l == 0L);
        }
        if(balance < 0) {
            balance = 0;
        }
    }
    
    // TODO: Caching
    // TODO: This is kinda dumb tbqh
    @JsonIgnore
    public Account getAccount() {
        //noinspection ConstantConditions
        return Mewna.getInstance().getDatabase().getAccountByDiscordId(id).get();
    }
}
