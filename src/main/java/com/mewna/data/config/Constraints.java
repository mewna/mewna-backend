package com.mewna.data.config;

/**
 * @author amy
 * @since 5/9/18.
 */
public enum Constraints {
    // Type constraints
    TYPE_STRING,
    TYPE_BOOLEAN,
    TYPE_MAP,
    TYPE_LIST,
    
    // Value constraints
    VALUE_NOT_NULL,
    VALUE_STRING_NOT_EMPTY,
    
    // String constraints (they're special)
    STRING_LEN_8,
    STRING_LEN_16,
    STRING_LEN_32,
    STRING_LEN_2K,
}
