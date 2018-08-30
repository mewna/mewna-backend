package com.mewna.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.cache.entity.Guild;
import com.mewna.plugin.CommandContext;
import com.mewna.plugin.plugins.economy.Box;
import com.mewna.plugin.plugins.economy.Item;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
@GIndex({"id", "guildXp", "ownedBackgroundPacks", "balance"})
@SuppressWarnings({"unused", "RedundantFieldInitialization", "WeakerAccess", "UnusedReturnValue"})
public class Player {
    public static final long MAX_INV_WEIGHT = 10_000;
    // We don't use the constant here because it might change later on
    public static final BigDecimal BASE_CLICKRATE = new BigDecimal(1);
    
    @PrimaryKey
    private String id;
    private long balance = 0L;
    private long lastDaily = 0L;
    private long dailyStreak = 0L;
    private Map<String, Long> guildXp = new HashMap<>();
    private long globalXp = 0L;
    private Map<Box, Long> boxes = new HashMap<>();
    private Map<Item, Long> items = new HashMap<>();
    private Votes votes = new Votes();
    @JsonProperty("clickerData")
    private ClickerData clickerData = new ClickerData();
    
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
        /*
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
        */
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
                items.put(item, items.get(item) - count);
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
    
    // Clicker
    
    public ClickerData getClickerData() {
        if(clickerData == null) {
            clickerData = new ClickerData();
            // Save it to the DB now because it matters xux;;;
            Mewna.getInstance().getDatabase().savePlayer(this);
        }
        return clickerData;
    }
    
    // Other
    
    /**
     * Clean up the player's data so that it's "safe" to put in the DB
     */
    public void cleanup() {
        if(items != null) {
            items.values().removeIf(l -> l <= 0L);
        }
        if(boxes != null) {
            boxes.values().removeIf(l -> l <= 0L);
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
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Votes {
        // $ E L L O U T
        // uwu
        private long dblorg;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class ClickerData {
        /**
         * It's too expensive to constantly be simulating this, so instead, we
         * mark the time the player last checked their clicker stats, and then
         * use the time delta to calculate the increase based on the base rate
         * of increase.
         */
        private long lastCheck = -1L;
        
        private BigDecimal totalClicks = BigDecimal.ZERO;
        
        private Set<ClickerTiers> unlockedTiers = new HashSet<>();
        
        private Map<ClickerUpgrades, Long> upgrades = new HashMap<>();
    }
    
    public enum ClickerTiers {
        
        // @formatter:off
        T1  ("Nine Lives",         BigDecimal.valueOf(                        0L)), // 0
        T2  ("Just Nyan More",     BigDecimal.valueOf(                      100L)), // 100
        T3  ("Purrenial Interest", BigDecimal.valueOf(                    1_000L)), // 1k
        T4  ("Hitting the Catnip", BigDecimal.valueOf(                   10_000L)), // 10k
        T5  ("Found YouTube",      BigDecimal.valueOf(                  100_000L)), // 100k
        T6  ("Claws Out",          BigDecimal.valueOf(                1_000_000L)), // 1m
        T7  ("Soft Pads",          BigDecimal.valueOf(            1_000_000_000L)), // 1b
        T8  ("Found By YouTube",   BigDecimal.valueOf(        1_000_000_000_000L)), // 1t
        T9  ("Caliconnoisseur",    BigDecimal.valueOf(    1_000_000_000_000_000L)), // 1qd
        T10 ("Uses Litterbox",     BigDecimal.valueOf(1_000_000_000_000_000_000L)), // 1qt
        ; // Long.MAX_VALUE for comparison            9_223_372_036_854_775_807L
        // @formatter:on
        
        @Getter
        private final String name;
        @Getter
        private final BigDecimal minValue;
    
        ClickerTiers(final String name, final BigDecimal minValue) {
            this.name = name;
            this.minValue = minValue;
        }
    }
    
    public enum ClickerUpgrades {
    
    }
}
