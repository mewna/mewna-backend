/**
 * Copyright 2014-2016 the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.mewna.plugin.plugins.dnd.dice.notation.operand;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.mewna.plugin.plugins.dnd.dice.Dice;
import com.mewna.plugin.plugins.dnd.dice.roller.Roller;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Operand for using dice values on a dice notation expression.
 * <p>
 * The value from a dice operand is random, and will be com.mewna.plugin.plugins.dnd.dice.generated each time it
 * is acquired.
 *
 * @author Bernardo Martínez Garrido
 * @see Dice
 */
public final class DefaultDiceOperand implements DiceOperand {
    
    /**
     * Roller to generate the random value from the dice.
     */
    private final Roller diceRoller;
    
    /**
     * Operand dice.
     * <p>
     * This will be used to generate the random value this operand uses.
     */
    private final Dice operandDice;
    
    /**
     * Constructs a dice operand with the specified dice and roller.
     *
     * @param dice   dice for the operand
     * @param roller roller for the dice
     */
    public DefaultDiceOperand(final Dice dice, final Roller roller) {
        super();
        
        operandDice = checkNotNull(dice, "Received a null pointer as dice");
        diceRoller = checkNotNull(roller, "Received a null pointer as roller");
    }
    
    @Override
    public final boolean equals(final Object obj) {
        if(this == obj) {
            return true;
        }
        
        if(obj == null) {
            return false;
        }
        
        if(getClass() != obj.getClass()) {
            return false;
        }
        
        final DefaultDiceOperand other;
        
        other = (DefaultDiceOperand) obj;
        
        return Objects.equal(operandDice, other.operandDice);
    }
    
    @Override
    public final Dice getDice() {
        return operandDice;
    }
    
    @Override
    public final String getExpression() {
        return String.format("%dd%d", getDice().getQuantity(),
                getDice().getSides());
    }
    
    @Override
    public final Integer getValue() {
        final Iterable<Integer> rolls;
        Integer result;
        
        rolls = getRoller().roll(getDice());
        
        result = 0;
        for(final Integer roll : rolls) {
            result += roll;
        }
        
        return result;
    }
    
    @Override
    public final int hashCode() {
        return Objects.hashCode(operandDice);
    }
    
    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("dice", operandDice)
                .toString();
    }
    
    /**
     * Returns the roller used to generate random values from the dice.
     *
     * @return the roller used for the random values
     */
    private final Roller getRoller() {
        return diceRoller;
    }
}
