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

package com.mewna.plugin.plugins.dnd.dice.notation.operation;

import com.mewna.plugin.plugins.dnd.dice.notation.DiceNotationExpression;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Addition operation.
 * <p>
 * This subtracts the value of an operand from the value of the other operand.
 * <p>
 * As with any other subtraction, the right operand's value will be subtracted
 * from the left one's.
 * 
 * @author Bernardo Martínez Garrido
 */
public final class SubtractionOperation implements com.mewna.plugin.plugins.dnd.dice.notation.operation.BinaryOperation {

    /**
     * Left sided operand.
     */
    private final DiceNotationExpression operandLeft;

    /**
     * Right sided operand.
     */
    private final DiceNotationExpression operandRight;

    /**
     * Constructs a subtraction operation with the specified operands.
     * 
     * @param left
     *            the left sided operand
     * @param right
     *            the right sided operand
     */
    public SubtractionOperation(final DiceNotationExpression left,
            final DiceNotationExpression right) {
        super();

        operandLeft = checkNotNull(left,
                "Received a null pointer as left operand");
        operandRight = checkNotNull(right,
                "Received a null pointer as right operand");
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final SubtractionOperation other;

        other = (SubtractionOperation) obj;

        return Objects.equal(operandLeft, other.operandLeft)
                && Objects.equal(operandRight, other.operandRight);
    }

    @Override
    public final String getExpression() {
        final String left;  // Left side operand as a string
        final String right; // Right side operand as a string

        left = getLeft().getExpression();
        right = getRight().getExpression();

        return String.format("%s-%s", left, right);
    }

    @Override
    public final DiceNotationExpression getLeft() {
        return operandLeft;
    }

    @Override
    public final DiceNotationExpression getRight() {
        return operandRight;
    }

    /**
     * Returns the left value after subtracting the right value from it.
     * 
     * @return the left value minus the right value
     */
    @Override
    public final Integer getValue() {
        return getLeft().getValue() - getRight().getValue();
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(operandLeft, operandRight);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("left", operandLeft)
                .add("right", operandRight).toString();
    }

}
