// Generated from chat/amy/util/dice/com.mewna.plugin.plugins.dnd.dice.generated/DiceNotationGrammar.g4 by ANTLR 4.5.1
package com.mewna.plugin.plugins.dnd.dice.generated;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DiceNotationGrammarLexer extends Lexer {
    public static final int
            OPERATOR = 1, ADD = 2, SUB = 3, DSEPARATOR = 4, DIGIT = 5;
    public static final String[] ruleNames = {
            "OPERATOR", "ADD", "SUB", "DSEPARATOR", "DIGIT"
    };
    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;
    public static final String _serializedATN =
            "\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\2\7\34\b\1\4\2\t\2" +
                    "\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\3\2\5\2\20\n\2\3\3\3\3\3\4\3\4\3" +
                    "\5\3\5\3\6\6\6\31\n\6\r\6\16\6\32\2\2\7\3\3\5\4\7\5\t\6\13\7\3\2\3\4\2" +
                    "FFff\35\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\3" +
                    "\17\3\2\2\2\5\21\3\2\2\2\7\23\3\2\2\2\t\25\3\2\2\2\13\30\3\2\2\2\r\20" +
                    "\5\5\3\2\16\20\5\7\4\2\17\r\3\2\2\2\17\16\3\2\2\2\20\4\3\2\2\2\21\22\7" +
                    "-\2\2\22\6\3\2\2\2\23\24\7/\2\2\24\b\3\2\2\2\25\26\t\2\2\2\26\n\3\2\2" +
                    "\2\27\31\4\62;\2\30\27\3\2\2\2\31\32\3\2\2\2\32\30\3\2\2\2\32\33\3\2\2" +
                    "\2\33\f\3\2\2\2\5\2\17\32\2";
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
    public static String[] modeNames = {
            "DEFAULT_MODE"
    };
    
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
    
    public DiceNotationGrammarLexer(CharStream input) {
        super(input);
        removeErrorListener(ConsoleErrorListener.INSTANCE);
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
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
    public String[] getModeNames() {
        return modeNames;
    }
    
    @Override
    public ATN getATN() {
        return _ATN;
    }
}