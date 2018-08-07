package com.mewna.plugin.plugins.dnd.dice.notation;

/**
 * A dice notation expression.
 * <p>
 * Dice notation expressions are meant to generate a value. Note that this value
 * may be com.mewna.plugin.plugins.dnd.dice.generated at random, and as such it can be different each time it is
 * acquired.
 * <p>
 * It is also possible getting the string representation of the dice notation
 * expression it represents.
 *
 * @author Bernardo Martínez Garrido
 */
@SuppressWarnings("unused")
public interface DiceNotationExpression {
    
    /**
     * Returns the expression as a string.
     * <p>
     * This will be the dice expression as it is written, for example "2+1d6".
     *
     * @return the expression as a string
     */
    String getExpression();
    
    /**
     * Returns the integer value of the expression.
     * <p>
     * As the dice notation expressions are meant to generate random values, the
     * result of this methods may be different each time it is acquired.
     *
     * @return the integer value of the expression
     */
    Integer getValue();
}
