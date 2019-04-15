package com.mewna.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mewna.Mewna;
import com.mewna.accounts.Account;
import com.mewna.catnip.entity.guild.Guild;
import com.mewna.plugin.commands.CommandContext;
import com.mewna.plugin.plugins.economy.Box;
import com.mewna.plugin.plugins.economy.Item;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.*;

import java.beans.Transient;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.mewna.data.Player.ClickerTiers.*;

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
@SuppressWarnings({"RedundantFieldInitialization", "unused", "WeakerAccess", "UnusedReturnValue"})
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
        return getXp(guild.id());
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
    
    public void setLocalXp(final String id, final long amount) {
        guildXp.put(id, amount);
        if(guildXp.get(id) < 0L) {
            guildXp.put(id, 0L);
        }
    }
    
    public void incrementLocalXp(final Guild guild, final long amount) {
        incrementLocalXp(guild.id(), amount);
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
            Mewna.getInstance().database().savePlayer(this);
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
    
    @JsonIgnore
    public Account getAccount() {
        // noinspection OptionalGetWithoutIsPresent
        return Mewna.getInstance().database().getAccountByDiscordId(id).get();
    }
    
    // Inner classes
    
    public enum ClickerTiers {
        // @formatter:off
        T1  (1,  "Nine Lives",         BigDecimal.valueOf(                        0L)), // 0
        T2  (2,  "Just Nyan More",     BigDecimal.valueOf(                      100L)), // 100
        T3  (3,  "Purrenial Interest", BigDecimal.valueOf(                    1_000L)), // 1k
        T4  (4,  "Hitting the Catnip", BigDecimal.valueOf(                   10_000L)), // 10k
        T5  (5,  "Found YouTube",      BigDecimal.valueOf(                  100_000L)), // 100k
        T6  (6,  "Claws Out",          BigDecimal.valueOf(                1_000_000L)), // 1m
        T7  (7,  "Soft Pads",          BigDecimal.valueOf(            1_000_000_000L)), // 1b
        T8  (8,  "Found By YouTube",   BigDecimal.valueOf(        1_000_000_000_000L)), // 1t
        T9  (9,  "Caliconnoisseur",    BigDecimal.valueOf(    1_000_000_000_000_000L)), // 1qd
        T10 (10, "Uses Litterbox",     BigDecimal.valueOf(1_000_000_000_000_000_000L)), // 1qt
        ; // Long.MAX_VALUE for comparison                            9_223_372_036_854_775_807L
    
        // @formatter:on
        @Getter
        private final int rank;
        
        @Getter
        private final String name;
        
        @Getter
        private final BigDecimal minValue;
        
        ClickerTiers(final int rank, final String name, final BigDecimal minValue) {
            this.rank = rank;
            this.name = name;
            this.minValue = minValue;
        }
    
        public String tierString() {
            return "T" + rank;
        }
    }
    
    public enum ClickerBuildings {
        // @formatter:off
        MINER(              "miner",             "A Mewna who mines for tato.",                                         T1, 20L,    BigDecimal.valueOf(2L),       Item.PICKAXE),
        FERTILIZER(         "fertilizer",        "Fertilizer to grow more tato in the mines.",                          T1, 250L,   BigDecimal.valueOf(25L),      Item.PASTA, Item.RAMEN, Item.DONUT),
        FRENCH_FRY_MACHINE( "frenchfrymachine",  "Turn tato into french fries to boost output.",                        T1, 2000L,  BigDecimal.valueOf(200L),     Item.FRIES, Item.HOTDOG, Item.BOOT),
        POTATO_CHIP_FACTORY("potatochipfactory", "Turn tato into french chips to boost output.",                        T2, 5000L,  BigDecimal.valueOf(1000L),    Item.FRIES, Item.BURGER, Item.BOOT),
        FOOD_TRUCK(         "foodtruck",         "Cart out tato faster for extra income.",                              T3, 10000L, BigDecimal.valueOf(30000L),   Item.FRIES, Item.BURGER, Item.HOTDOG, Item.PASTA, Item.RAMEN, Item.FISH, Item.TROPICAL_FISH, Item.BURRITO),
        TATO_TEMPLE(        "tatotemple",        "Worship the Almighty Tato for mining blessings.",                     T4, 30000L, BigDecimal.valueOf(50000L),   Item.FRIES, Item.BOOT, Item.WEED, Item.FISH, Item.PICKAXE, Item.FISHING_ROD, Item.STAR, Item.DIAMOND, Item.PIZZA, Item.WHALE),
        TATO_PORTAL(        "tatoportal",        "Open a portal to the Tato Dimension for even more tato!",             T7, 10000L, BigDecimal.valueOf(250000L),  Item.FRIES, Item.BOOT, Item.WEED, Item.FISH, Item.PICKAXE, Item.FISHING_ROD, Item.STAR, Item.DIAMOND, Item.PIZZA, Item.WHALE, Item.RAMEN, Item.TACO),
        ASCENDED_MEWNA(     "ascendedmewna",     "A Mewna who has ascended to godhood and produces tato from nothing.", T8, 30000L, BigDecimal.valueOf(2000000L), Item.FRIES, Item.BOOT, Item.WEED, Item.FISH, Item.PICKAXE, Item.FISHING_ROD, Item.STAR, Item.DIAMOND, Item.PIZZA, Item.WHALE, Item.RAMEN, Item.TACO),
        ;
    
        // @formatter:off
        @Getter
        private final String name;
        @Getter
        private final String desc;
        @Getter
        private final ClickerTiers tier;
        @Getter
        private final long flowers;
        @Getter
        private final BigDecimal output;
        @Getter
        private final Item[] items;
        
        ClickerBuildings(final String name, final String desc, final ClickerTiers tier, final long flowers,
                         final BigDecimal output, final Item... items) {
            this.name = name;
            this.desc = desc;
            this.tier = tier;
            this.flowers = flowers;
            this.output = output;
            this.items = items;
        }
    
        public static ClickerBuildings byName(final String name) {
            for(final ClickerBuildings u : values()) {
                if(u.name.equalsIgnoreCase(name)) {
                    return u;
                }
            }
            return null;
        }
    
        public boolean playerHasTier(final Player player) {
            return player.clickerData.getTier().rank >= tier.rank;
        }
    }
    
    public enum ClickerUpgrades {
        // @formatter:off
        POTATO_SLICER(   "potatoslicer",   "Potato slicers help your Mewnas get tato faster.",                               T1, 10L,     Item.PICKAXE),
        POTATO_MASHER(   "potatomasher",   "Potato mashers increase your Mewnas' tato production",                           T1, 100L,    Item.BOOT, Item.COMET),
        FRENCH_FRY_OIL(  "frenchfryoil",   "French fry oil makes fry machines output more.",                                 T1, 500L,    Item.COMET, Item.FRIES),
        POTATO_CHIP_SALT("potatochipsalt", "Potato chip salt causes factories to produce more.",                             T1, 1000L,   Item.STAR, Item.FRIES),
        EXTRA_SEASONING( "extraseasoning", "Extra seasoning makes your food tastier and worth more tato.",                   T1, 10000L,  Item.DIAMOND, Item.FRIES),
        POTATO_SALAD(    "potatosalad",    "Mewnas use potato salad for energy when deep in the tato mines.",                T1, 50000L,  Item.WEED, Item.DIAMOND),
        CUTER_EARS(      "cuterears",      "Cuter ears make your Mewnas irresistable, and people will give them free tato.", T1, 100000L, Item.STAR, Item.DIAMOND),
        ;
    
        // @formatter:on
        @Getter
        private final String name;
        @Getter
        private final String desc;
        @Getter
        private final ClickerTiers tier;
        @Getter
        private final long flowers;
    
        @Getter
        private final Item[] items;
    
        ClickerUpgrades(final String name, final String desc, final ClickerTiers tier, final long flowers, final Item... items) {
            this.name = name;
            this.desc = desc;
            this.tier = tier;
            this.flowers = flowers;
            this.items = items;
        }
    
        public static ClickerUpgrades byName(final String name) {
            for(final ClickerUpgrades u : values()) {
                if(u.name.equalsIgnoreCase(name)) {
                    return u;
                }
            }
            return null;
        }
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
        
        private Map<ClickerBuildings, Long> buildings = new HashMap<>();
        
        private Set<ClickerUpgrades> upgrades = new HashSet<>();
        
        private long food = 1000L;
        
        @Transient
        @JsonIgnore
        public BigDecimal getTatoPerSecond() {
            BigDecimal res = BASE_CLICKRATE;
            for(final Entry<ClickerBuildings, Long> entry : buildings.entrySet()) {
                if(entry.getValue() == 0L) {
                    continue;
                }
                BigDecimal out = entry.getKey().getOutput();
                switch(entry.getKey()) {
                    case MINER: {
                        if(upgrades.contains(ClickerUpgrades.POTATO_SLICER)) {
                            out = out.multiply(BigDecimal.valueOf(2L));
                        }
                        if(upgrades.contains(ClickerUpgrades.POTATO_MASHER)) {
                            out = out.multiply(BigDecimal.valueOf(2L));
                        }
                        break;
                    }
                    case FRENCH_FRY_MACHINE: {
                        if(upgrades.contains(ClickerUpgrades.FRENCH_FRY_OIL)) {
                            out = out.multiply(BigDecimal.valueOf(2L));
                        }
                        break;
                    }
                    case FOOD_TRUCK: {
                        if(upgrades.contains(ClickerUpgrades.EXTRA_SEASONING)) {
                            out = out.multiply(BigDecimal.valueOf(2L));
                        }
                        break;
                    }
                    case POTATO_CHIP_FACTORY: {
                        if(upgrades.contains(ClickerUpgrades.POTATO_CHIP_SALT)) {
                            out = out.multiply(BigDecimal.valueOf(2L));
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
                if(upgrades.contains(ClickerUpgrades.POTATO_SALAD)) {
                    out = out.multiply(BigDecimal.valueOf(3L));
                }
                if(upgrades.contains(ClickerUpgrades.CUTER_EARS)) {
                    out = out.multiply(BigDecimal.valueOf(10L));
                }
                res = res.add(out.multiply(BigDecimal.valueOf(entry.getValue())));
            }
            return res;
        }
        
        @Transient
        @JsonIgnore
        @SuppressWarnings("UnnecessarilyQualifiedStaticallyImportedElement")
        public ClickerTiers getTier() {
            for(int i = 0; i < ClickerTiers.values().length; i++) {
                final ClickerTiers tier = ClickerTiers.values()[i];
                if(tier.getMinValue().compareTo(totalClicks) > 0) {
                    if(i > 0) {
                        return ClickerTiers.values()[i - 1];
                    } else {
                        return ClickerTiers.values()[i];
                    }
                }
            }
            return T1;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Votes {
        // $ E L L O U T
        // uwu
        private long dblorg;
    }
}
