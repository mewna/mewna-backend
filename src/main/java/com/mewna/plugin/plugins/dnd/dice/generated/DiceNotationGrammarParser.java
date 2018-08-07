// Generated from chat/amy/util/dice/com.mewna.plugin.plugins.dnd.dice.generated/DiceNotationGrammar.g4 by ANTLR 4.5.1
package com.mewna.plugin.plugins.dnd.dice.generated;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DiceNotationGrammarParser extends Parser {
    public static final int
            OPERATOR = 1, ADD = 2, SUB = 3, DSEPARATOR = 4, DIGIT = 5;
    public static final int
            RULE_parse = 0, RULE_function = 1, RULE_binaryOp = 2, RULE_dice = 3;
    public static final String[] ruleNames = {
            "parse", "function", "binaryOp", "dice"
    };
    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;
    public static final String _serializedATN =
            "\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\7\37\4\2\t\2\4\3" +
                    "\t\3\4\4\t\4\4\5\t\5\3\2\3\2\3\3\3\3\3\3\5\3\20\n\3\3\4\3\4\3\4\3\4\3" +
                    "\4\3\4\3\4\5\4\31\n\4\3\5\3\5\3\5\3\5\3\5\2\2\6\2\4\6\b\2\2\35\2\n\3\2" +
                    "\2\2\4\17\3\2\2\2\6\30\3\2\2\2\b\32\3\2\2\2\n\13\5\4\3\2\13\3\3\2\2\2" +
                    "\f\20\5\b\5\2\r\20\5\6\4\2\16\20\7\7\2\2\17\f\3\2\2\2\17\r\3\2\2\2\17" +
                    "\16\3\2\2\2\20\5\3\2\2\2\21\22\5\b\5\2\22\23\7\3\2\2\23\24\5\4\3\2\24" +
                    "\31\3\2\2\2\25\26\7\7\2\2\26\27\7\3\2\2\27\31\5\4\3\2\30\21\3\2\2\2\30" +
                    "\25\3\2\2\2\31\7\3\2\2\2\32\33\7\7\2\2\33\34\7\6\2\2\34\35\7\7\2\2\35" +
                    "\t\3\2\2\2\4\17\30";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());
    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    private static final String[] _LITERAL_NAMES = {
            null, null, "'+'", "'-'"
    };
    private static final String[] _SYMBOLIC_NAMES = {
            null, "OPERATOR", "ADD", "SUB", "DSEPARATOR", "DIGIT"
    };
    public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);
    
    static {
        RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION);
    }
    
    static {
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for(int i = 0; i < tokenNames.length; i++) {
            tokenNames[i] = VOCABULARY.getLiteralName(i);
            if(tokenNames[i] == null) {
                tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }
            
            if(tokenNames[i] == null) {
                tokenNames[i] = "<INVALID>";
            }
        }
    }
    
    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for(int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
    
    public DiceNotationGrammarParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }
    
    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }
    
    @Override
    
    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }
    
    @Override
    public String getGrammarFileName() {
        return "DiceNotationGrammar.g4";
    }
    
    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }
    
    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }
    
    @Override
    public ATN getATN() {
        return _ATN;
    }
    
    public final ParseContext parse() throws RecognitionException {
        ParseContext _localctx = new ParseContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_parse);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(8);
                function();
            }
        } catch(RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }
    
    public final FunctionContext function() throws RecognitionException {
        FunctionContext _localctx = new FunctionContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_function);
        try {
            setState(13);
            _errHandler.sync(this);
            switch(getInterpreter().adaptivePredict(_input, 0, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(10);
                    dice();
                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(11);
                    binaryOp();
                }
                break;
                case 3:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(12);
                    match(DIGIT);
                }
                break;
            }
        } catch(RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }
    
    public final BinaryOpContext binaryOp() throws RecognitionException {
        BinaryOpContext _localctx = new BinaryOpContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_binaryOp);
        try {
            setState(22);
            _errHandler.sync(this);
            switch(getInterpreter().adaptivePredict(_input, 1, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(15);
                    dice();
                    setState(16);
                    match(OPERATOR);
                    setState(17);
                    function();
                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(19);
                    match(DIGIT);
                    setState(20);
                    match(OPERATOR);
                    setState(21);
                    function();
                }
                break;
            }
        } catch(RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }
    
    public final DiceContext dice() throws RecognitionException {
        DiceContext _localctx = new DiceContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_dice);
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(24);
                match(DIGIT);
                setState(25);
                match(DSEPARATOR);
                setState(26);
                match(DIGIT);
            }
        } catch(RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }
    
    public static class ParseContext extends ParserRuleContext {
        public ParseContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        
        public FunctionContext function() {
            return getRuleContext(FunctionContext.class, 0);
        }
        
        @Override
        public int getRuleIndex() {
            return RULE_parse;
        }
        
        @Override
        public void enterRule(ParseTreeListener listener) {
            if(listener instanceof DiceNotationGrammarListener) ((DiceNotationGrammarListener) listener).enterParse(this);
        }
        
        @Override
        public void exitRule(ParseTreeListener listener) {
            if(listener instanceof DiceNotationGrammarListener) ((DiceNotationGrammarListener) listener).exitParse(this);
        }
    }
    
    public static class FunctionContext extends ParserRuleContext {
        public FunctionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        
        public DiceContext dice() {
            return getRuleContext(DiceContext.class, 0);
        }
        
        public BinaryOpContext binaryOp() {
            return getRuleContext(BinaryOpContext.class, 0);
        }
        
        public TerminalNode DIGIT() {
            return getToken(DiceNotationGrammarParser.DIGIT, 0);
        }
        
        @Override
        public int getRuleIndex() {
            return RULE_function;
        }
        
        @Override
        public void enterRule(ParseTreeListener listener) {
            if(listener instanceof DiceNotationGrammarListener) ((DiceNotationGrammarListener) listener).enterFunction(this);
        }
        
        @Override
        public void exitRule(ParseTreeListener listener) {
            if(listener instanceof DiceNotationGrammarListener) ((DiceNotationGrammarListener) listener).exitFunction(this);
        }
    }

    public static class BinaryOpContext extends ParserRuleContext {
        public BinaryOpContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        
        public DiceContext dice() {
            return getRuleContext(DiceContext.class, 0);
        }
        
        public TerminalNode OPERATOR() {
            return getToken(DiceNotationGrammarParser.OPERATOR, 0);
        }
        
        public FunctionContext function() {
            return getRuleContext(FunctionContext.class, 0);
        }
        
        public TerminalNode DIGIT() {
            return getToken(DiceNotationGrammarParser.DIGIT, 0);
        }
        
        @Override
        public int getRuleIndex() {
            return RULE_binaryOp;
        }
        
        @Override
        public void enterRule(ParseTreeListener listener) {
            if(listener instanceof DiceNotationGrammarListener) ((DiceNotationGrammarListener) listener).enterBinaryOp(this);
        }
        
        @Override
        public void exitRule(ParseTreeListener listener) {
            if(listener instanceof DiceNotationGrammarListener) ((DiceNotationGrammarListener) listener).exitBinaryOp(this);
        }
    }
    
    public static class DiceContext extends ParserRuleContext {
        public DiceContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        
        public List<TerminalNode> DIGIT() {
            return getTokens(DiceNotationGrammarParser.DIGIT);
        }
        
        public TerminalNode DIGIT(int i) {
            return getToken(DiceNotationGrammarParser.DIGIT, i);
        }
        
        public TerminalNode DSEPARATOR() {
            return getToken(DiceNotationGrammarParser.DSEPARATOR, 0);
        }
        
        @Override
        public int getRuleIndex() {
            return RULE_dice;
        }
        
        @Override
        public void enterRule(ParseTreeListener listener) {
            if(listener instanceof DiceNotationGrammarListener) ((DiceNotationGrammarListener) listener).enterDice(this);
        }
        
        @Override
        public void exitRule(ParseTreeListener listener) {
            if(listener instanceof DiceNotationGrammarListener) ((DiceNotationGrammarListener) listener).exitDice(this);
        }
    }
}