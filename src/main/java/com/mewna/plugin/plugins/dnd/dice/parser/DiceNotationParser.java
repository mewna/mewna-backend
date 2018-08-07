package com.mewna.plugin.plugins.dnd.dice.parser;

import com.mewna.plugin.plugins.dnd.dice.notation.DiceNotationExpression;

/**
 * Transforms a dice notation expression, received as a string, into the dice
 * notation model.
 * <p>
 * The returned object is expected to be the root node of a tree made up by dice
 * notation model objects.
 *
 * @author Bernardo Martínez Garrido
 */
@FunctionalInterface
public interface DiceNotationParser {
    
    /**
     * Transforms a dice notation expression into the dice notation model.
     *
     * @param expression the expression to parse
     *
     * @return a dice notation expression object
     */
    DiceNotationExpression parse(final String expression);
}
