package com.mewna.plugin.plugins.economy;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import static com.mewna.plugin.plugins.economy.Item.*;

/**
 * @author amy
 * @since 7/22/18.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class LootTables {
    public static final Map<Item, Integer> FOOD = ImmutableMap.<Item, Integer>builder()
            .put(BURGER, 10)
            .put(FRIES, 10)
            .put(HOTDOG, 10)
            .put(PIZZA, 10)
            .put(DONUT, 10)
            .put(RAMEN, 10)
            .put(BURRITO, 10)
            .put(TACO, 10)
            .put(PASTA, 10)
            .build();
    
    public static final Map<Item, Integer> LUNCH = ImmutableMap.of(
            BURGER, 10,
            FRIES, 5,
            HOTDOG, 20,
            PIZZA, 10,
            BURRITO, 20
    );
    
    public static final Map<Item, Integer> GEMS = ImmutableMap.of(
            DIAMOND, 5,
            COMET, 20,
            STAR, 30,
            SNOWFLAKE, 50,
            SPARKLES, 40
    );
    
    public static final Map<Item, Integer> TOOLS = ImmutableMap.of(
            PICKAXE, 50,
            FISHING_ROD, 50
    );
    
    public static final Map<Item, Integer> FISHING = ImmutableMap.of(
            FISH, 75,
            BOOT, 50,
            WEED, 50,
            TROPICAL_FISH, 30,
            WHALE, 10
    );
    
    public static final Map<Item, Integer> COLLECTIBLE = ImmutableMap.of(
            DIE, 10,
            EIGHT_BALL, 10,
            LOTTERY_TICKET, 10,
            SLOT_MACHINE, 10
    );
    
    private LootTables() {
    }
    
    public static List<Item> generateLoot(final Map<Item, Integer> table, final int min, final int max) {
        if(min > max) {
            throw new IllegalArgumentException("min > max!");
        }
        if(tlr() % 100 <= 10) {
            return Collections.emptyList();
        }
        final List<Item> loot = new ArrayList<>();
        
        while(loot.size() < min) {
            for(final Entry<Item, Integer> lootItem : table.entrySet()) {
                if(chance(lootItem.getValue())) {
                    loot.add(lootItem.getKey());
                }
                if(loot.size() >= min) {
                    break;
                }
            }
        }
        
        while(loot.size() < max) {
            if(chance(50)) {
                break;
            }
            for(final Entry<Item, Integer> lootItem : table.entrySet()) {
                if(chance(lootItem.getValue())) {
                    loot.add(lootItem.getKey());
                }
                if(chance(50)) {
                    break;
                }
                if(loot.size() == max) {
                    break;
                }
            }
        }
        
        return loot;
    }
    
    private static int tlr() {
        return Math.abs(ThreadLocalRandom.current().nextInt());
    }
    
    public static boolean chance(final int number) {
        return tlr() % 100 < number;
    }
}
