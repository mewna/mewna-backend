package com.mewna.plugin.plugins.dnd.dice;

/**
 * A group of dice, all with the same number of sides.
 * <p>
 * This is meant to represent a group of dice by itself, to handle complex
 * notation the classes in the {@link com.mewna.plugin.plugins.dnd.dice
 * notation} package should be used.
 * <p>
 * The number of dice are expected to be positive or zero, and the number of
 * sides greater than zero, as any other value would make no sense.
 * <p>
 * No other limitation is expected. In the real world the number of sides which
 * a die may physically have are limited by the rules of geometry, but there is
 * no reason to take care of that.
 * <p>
 * The main use of a dice is generating a random number. While this is not
 * supported by the interface, any implementation of
 * {@link com.mewna.plugin.plugins.dnd.dice.roller.Roller Roller} will take care of
 * that concern.
 * 
 * @author Bernardo Martínez Garrido
 * @see com.mewna.plugin.plugins.dnd.dice.roller.Roller Roller
 */
public interface Dice {

    /**
     * Returns the number of dice which compose this group.
     * <p>
     * This is expected to be a positive value or zero.
     * 
     * @return the number of dice being rolled
     */
    public Integer getQuantity();

    /**
     * Returns the number of sides of the dice in the group.
     * <p>
     * All the dice will have this same number of sides.
     * <p>
     * This is expected to be a positive value greater than zero.
     * 
     * @return the dice's number of sides
     */
    public Integer getSides();

}
