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

package com.mewna.plugin.plugins.dnd.dice.parser.listener;

import com.mewna.plugin.plugins.dnd.dice.DefaultDice;
import com.mewna.plugin.plugins.dnd.dice.Dice;
import com.mewna.plugin.plugins.dnd.dice.generated.DiceNotationGrammarBaseListener;
import com.mewna.plugin.plugins.dnd.dice.notation.DiceNotationExpression;
import com.mewna.plugin.plugins.dnd.dice.notation.operand.DefaultDiceOperand;
import com.mewna.plugin.plugins.dnd.dice.notation.operand.DiceOperand;
import com.mewna.plugin.plugins.dnd.dice.notation.operand.IntegerOperand;
import com.mewna.plugin.plugins.dnd.dice.notation.operation.AdditionOperation;
import com.mewna.plugin.plugins.dnd.dice.notation.operation.BinaryOperation;
import com.mewna.plugin.plugins.dnd.dice.notation.operation.SubtractionOperation;
import com.mewna.plugin.plugins.dnd.dice.roller.DefaultRoller;
import com.mewna.plugin.plugins.dnd.dice.roller.Roller;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Stack;

import static com.mewna.plugin.plugins.dnd.dice.generated.DiceNotationGrammarParser.*;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Visitor for an ANTLR4 parser tree. It can return the fully parsed
 * {@link DiceNotationExpression}.
 * <p>
 * This {@code DiceNotationExpression} is the root for a tree representing the
 * expression received by the parser.
 * <p>
 * It contains a stack which stores the operands as they are parsed, this way
 * any operation, such as an addition, can acquire the latest operands, which
 * will be the ones it will employ.
 *
 * @author Bernardo Martínez Garrido
 */
public final class DefaultDiceExpressionBuilder extends
        DiceNotationGrammarBaseListener implements DiceExpressionBuilder {
    
    /**
     * Operator which indicates the operation is an addition.
     */
    private static final String ADDITION_OPERATOR = "+";
    
    /**
     * Operator which indicates the operation is a subtraction.
     */
    private static final String SUBTRACTION_OPERATOR = "-";
    
    /**
     * Roller for the dice expressions.
     * <p>
     * This is used as a dependency on the dice expressions, which require a
     * roller to generate their value.
     */
    private final Roller diceRoller;
    
    /**
     * Stack to store operands from the outer nodes in an operation.
     * <p>
     * For example, when parsing an addition operation this stack will hold both
     * operands being added together.
     */
    private final Stack<DiceNotationExpression> operandsStack = new Stack<>();
    
    /**
     * Root of the tree of dice notation model objects.
     * <p>
     * This will be updated as the tree is com.mewna.plugin.plugins.dnd.dice.generated, and will be the final
     * value returned by the builder.
     */
    private DiceNotationExpression root;
    
    /**
     * Default constructor.
     * <p>
     * It makes use of a {@link DefaultRoller}
     */
    public DefaultDiceExpressionBuilder() {
        this(new DefaultRoller());
    }
    
    /**
     * Constructs a builder with the specified roller.
     *
     * @param roller roller for the dice expressions
     */
    public DefaultDiceExpressionBuilder(final Roller roller) {
        super();
        
        diceRoller = checkNotNull(roller, "Received a null pointer as roller");
    }
    
    @Override
    public final void exitBinaryOp(final BinaryOpContext ctx) {
        checkNotNull(ctx, "Received a null pointer as context");
        
        setLatestExpression(getBinaryOperation(ctx));
    }
    
    @Override
    public final void exitDice(final DiceContext ctx) {
        checkNotNull(ctx, "Received a null pointer as context");
        
        setLatestExpression(getDiceOperand(ctx));
    }
    
    @Override
    public final void exitFunction(final FunctionContext ctx) {
        checkNotNull(ctx, "Received a null pointer as context");
        
        if(ctx.DIGIT() != null) {
            setLatestExpression(getIntegerOperand(ctx.DIGIT()));
        }
    }
    
    @Override
    public final DiceNotationExpression getDiceExpressionRoot() {
        return root;
    }
    
    /**
     * Sets the root for the tree of dice notation model objects.
     *
     * @param expression the expression to set as the root
     */
    private final void
    setDiceExpressionRoot(final DiceNotationExpression expression) {
        root = expression;
    }
    
    /**
     * Creates a binary operation from the parsed context data.
     *
     * @param ctx parsed context
     *
     * @return a binary operation
     */
    private final BinaryOperation
    getBinaryOperation(final BinaryOpContext ctx) {
        final BinaryOperation operation;    // Parsed binary operation
        final String operator;              // Operator
        final DiceNotationExpression left;  // Left operand
        final DiceNotationExpression right; // Right operand
        
        // Acquired operands
        right = getOperandsStack().pop();
        if(ctx.DIGIT() == null) {
            left = getOperandsStack().pop();
        } else {
            left = getIntegerOperand(ctx.DIGIT());
        }
        
        // Acquires operator
        operator = ctx.OPERATOR().getText();
        
        // Checks which kind of operation this is and creates it
        if(ADDITION_OPERATOR.equals(operator)) {
            operation = new AdditionOperation(left, right);
        } else if(SUBTRACTION_OPERATOR.equals(operator)) {
            operation = new SubtractionOperation(left, right);
        } else {
            throw new IllegalArgumentException(
                    String.format("The %s operator is invalid", operator));
        }
        
        return operation;
    }
    
    /**
     * Creates a dice operand from the parsed context data.
     *
     * @param ctx parsed context
     *
     * @return a dice operand
     */
    private final DiceOperand getDiceOperand(final DiceContext ctx) {
        final Dice dice;        // Parsed dice
        final Integer quantity; // Number of dice
        final Integer sides;    // Number of sides
        
        // Parses the dice data
        quantity = Integer.parseInt(ctx.DIGIT(0).getText());
        sides = Integer.parseInt(ctx.DIGIT(1).getText());
        
        // Creates the dice
        dice = new DefaultDice(quantity, sides);
        
        return new DefaultDiceOperand(dice, getRoller());
    }
    
    /**
     * Creates an integer operand from a terminal node.
     *
     * @param node terminal node
     *
     * @return an integer operand
     */
    private final IntegerOperand getIntegerOperand(final TerminalNode node) {
        final Integer value; // Parsed value
        
        // Parses the value
        value = Integer.parseInt(node.getText());
        
        return new IntegerOperand(value);
    }
    
    /**
     * Returns the operands stack.
     *
     * @return the operands stack
     */
    private final Stack<DiceNotationExpression> getOperandsStack() {
        return operandsStack;
    }
    
    /**
     * Returns the roller for the dice expressions.
     *
     * @return the roller for the dice expressions
     */
    private final Roller getRoller() {
        return diceRoller;
    }
    
    /**
     * Sets the received expression as the latest parsed expression.
     *
     * @param expression expression to set as the latest parsed expression
     */
    private final void
    setLatestExpression(final DiceNotationExpression expression) {
        // Adds to the operands stack
        getOperandsStack().push(expression);
        
        // Sets as the root
        setDiceExpressionRoot(getOperandsStack().peek());
    }
}
