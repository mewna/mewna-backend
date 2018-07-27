package com.mewna.plugin.plugins.economy;

import lombok.Getter;

/**
 * @author amy
 * @since 7/22/18.
 */
@Getter
public enum Item {
    // @formatter:off
    // Item         Type                    Name                Emote           Buy     Sell    Can buy     Weight
    
    // Food
    BURGER(         ItemType.FOOD,          "burger",           "\uD83C\uDF54", 10,     3,      true,       1),
    FRIES(          ItemType.FOOD,          "fries",            "\uD83C\uDF5F", 10,     3,      true,       1),
    HOTDOG(         ItemType.FOOD,          "hotdog",           "\uD83C\uDF2D", 10,     3,      true,       1),
    PIZZA(          ItemType.FOOD,          "pizza",            "\uD83C\uDF55", 10,     3,      true,       1),
    DONUT(          ItemType.FOOD,          "donut",            "\uD83C\uDF69", 10,     3,      true,       1),
    RAMEN(          ItemType.FOOD,          "ramen",            "\uD83C\uDF5C", 10,     3,      true,       1),
    BURRITO(        ItemType.FOOD,          "burrito",          "\uD83C\uDF2F", 10,     3,      true,       1),
    TACO(           ItemType.FOOD,          "taco",             "\uD83C\uDF2E", 10,     3,      true,       1),
    PASTA(          ItemType.FOOD,          "pasta",            "\uD83C\uDF5D", 10,     3,      true,       1),
    
    // Gem
    DIAMOND(        ItemType.GEM,           "diamond",          "\uD83D\uDC8E", 500,    50,     false,      1),
    COMET(          ItemType.GEM,           "comet",            "\u2604",       200,    35,     true,       1),
    STAR(           ItemType.GEM,           "star",             "\uD83C\uDF1F", 100,    20,     true,       1),
    SNOWFLAKE(      ItemType.GEM,           "snowflake",        "\u2744",       50,     10,     true,       1),
    SPARKLES(       ItemType.GEM,           "sparkles",         "\u2728",       50,     10,     true,       1),
    
    // Tool
    PICKAXE(        ItemType.TOOL,          "pickaxe",          "\u26CF",       25,     10,     true,       1),
    FISHING_ROD(    ItemType.TOOL,          "fishingrod",       "\uD83C\uDFA3", 25,     10,     true,       1),
    
    // Fish
    FISH(           ItemType.FISH,          "fish",             "\uD83D\uDC1F", 5,      2,      true,       1),
    WHALE(          ItemType.FISH,          "whale",            "\uD83D\uDC0B", 25,     8,     true,      1),
    TROPICAL_FISH(  ItemType.FISH,          "tropicalfish",     "\uD83D\uDC20", 20,     5,     true,       1),
    BOOT(           ItemType.FISH,          "boot",             "\uD83D\uDC62", 3,      1,      true,       1),
    WEED(           ItemType.FISH,          "weed",             "\uD83C\uDF31", 1,      1,      true,       1),
    
    // Collectible
    DIE(            ItemType.COLLECTIBLE,   "die",              "\uD83C\uDFB2", 0,      20,     false,      1),
    EIGHT_BALL(     ItemType.COLLECTIBLE,   "eightball",        "\uD83C\uDFB1", 0,      20,     false,      1),
    LOTTERY_TICKET( ItemType.COLLECTIBLE,   "lotteryticket",    "\uD83C\uDF9F", 0,      20,     false,      1),
    SLOT_MACHINE(   ItemType.COLLECTIBLE,   "slotmachine",      "\uD83C\uDFB0", 0,      20,     false,      1),
    ;
    // @formatter:on
    private final ItemType type;
    private final String name;
    private final String emote;
    private final long buyValue;
    private final long sellValue;
    private final boolean canBuy;
    private final int weight;
    
    Item(final ItemType type, final String name, final String emote, final long buyValue, final long sellValue,
         final boolean canBuy, final int weight) {
        this.type = type;
        this.name = name;
        this.emote = emote;
        this.buyValue = buyValue;
        this.sellValue = sellValue;
        this.canBuy = canBuy;
        this.weight = weight;
    }
    
    public enum ItemType {
        COLLECTIBLE,
        GEM,
        FISH,
        FOOD,
        TOOL,
    }
}
