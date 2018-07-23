package com.mewna.plugin.plugins.economy;

import lombok.Getter;

import java.util.Map;

/**
 * @author amy
 * @since 7/22/18.
 */
@Getter
public enum Box {
    TOOLBOX(BoxType.TOOL, "toolbox", 10, LootTables.TOOLS),
    LUNCHBOX(BoxType.LUNCH, "lunchbox", 10, LootTables.LUNCH),
    
    FOOD_CRATE(BoxType.UNCOMMON, "foodbox", 10, LootTables.FOOD),
    
    JEWELRY_BOX(BoxType.RARE, "jewelrybox", 10, LootTables.GEMS),
    FISH_BOX(BoxType.FISH, "fishbox", 10, LootTables.FISHING),
    
    COLLECTIBLES_CRATE(BoxType.UNCOMMON, "collectibles", 10, LootTables.COLLECTIBLE),
    
    ;
    private final BoxType type;
    private final String name;
    private final long sellValue;
    private final Map<Item, Integer> lootTable;
    
    Box(final BoxType type, final String name, final long sellValue, final Map<Item, Integer> lootTable) {
        this.type = type;
        this.name = name;
        this.sellValue = sellValue;
        this.lootTable = lootTable;
    }
    
    public enum BoxType {
        COMMON,
        UNCOMMON,
        RARE,
        TOOL,
        FISH,
        LUNCH,
    }
}
