// Generated from chat/amy/util/dice/com.mewna.plugin.plugins.dnd.dice.generated/DiceNotationGrammar.g4 by ANTLR 4.5.1
package com.mewna.plugin.plugins.dnd.dice.generated;

import com.mewna.plugin.plugins.dnd.dice.generated.DiceNotationGrammarParser.BinaryOpContext;
import com.mewna.plugin.plugins.dnd.dice.generated.DiceNotationGrammarParser.DiceContext;
import com.mewna.plugin.plugins.dnd.dice.generated.DiceNotationGrammarParser.FunctionContext;
import com.mewna.plugin.plugins.dnd.dice.generated.DiceNotationGrammarParser.ParseContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link DiceNotationGrammarParser}.
 */
public interface DiceNotationGrammarListener extends ParseTreeListener {
    /**
     * Enter a parse tree produced by {@link DiceNotationGrammarParser#parse}.
     *
     * @param ctx the parse tree
     */
    void enterParse(ParseContext ctx);
    
    /**
     * Exit a parse tree produced by {@link DiceNotationGrammarParser#parse}.
     *
     * @param ctx the parse tree
     */
    void exitParse(ParseContext ctx);
    
    /**
     * Enter a parse tree produced by {@link DiceNotationGrammarParser#function}.
     *
     * @param ctx the parse tree
     */
    void enterFunction(FunctionContext ctx);
    
    /**
     * Exit a parse tree produced by {@link DiceNotationGrammarParser#function}.
     *
     * @param ctx the parse tree
     */
    void exitFunction(FunctionContext ctx);
    
    /**
     * Enter a parse tree produced by {@link DiceNotationGrammarParser#binaryOp}.
     *
     * @param ctx the parse tree
     */
    void enterBinaryOp(BinaryOpContext ctx);
    
    /**
     * Exit a parse tree produced by {@link DiceNotationGrammarParser#binaryOp}.
     *
     * @param ctx the parse tree
     */
    void exitBinaryOp(BinaryOpContext ctx);
    
    /**
     * Enter a parse tree produced by {@link DiceNotationGrammarParser#dice}.
     *
     * @param ctx the parse tree
     */
    void enterDice(DiceContext ctx);
    
    /**
     * Exit a parse tree produced by {@link DiceNotationGrammarParser#dice}.
     *
     * @param ctx the parse tree
     */
    void exitDice(DiceContext ctx);
}