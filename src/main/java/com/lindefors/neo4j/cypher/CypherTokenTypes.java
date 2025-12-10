package com.lindefors.neo4j.cypher;

import com.intellij.psi.tree.IElementType;

public interface CypherTokenTypes {
    IElementType KEYWORD = new IElementType("KEYWORD", CypherLanguage.INSTANCE);
    IElementType IDENTIFIER = new IElementType("IDENTIFIER", CypherLanguage.INSTANCE);
    IElementType NUMBER = new IElementType("NUMBER", CypherLanguage.INSTANCE);
    IElementType STRING = new IElementType("STRING", CypherLanguage.INSTANCE);
    IElementType COMMENT = new IElementType("COMMENT", CypherLanguage.INSTANCE);
    IElementType PAREN_OPEN = new IElementType("PAREN_OPEN", CypherLanguage.INSTANCE);
    IElementType PAREN_CLOSE = new IElementType("PAREN_CLOSE", CypherLanguage.INSTANCE);
    IElementType BRACKET_OPEN = new IElementType("BRACKET_OPEN", CypherLanguage.INSTANCE);
    IElementType BRACKET_CLOSE = new IElementType("BRACKET_CLOSE", CypherLanguage.INSTANCE);
    IElementType BRACE_OPEN = new IElementType("BRACE_OPEN", CypherLanguage.INSTANCE);
    IElementType BRACE_CLOSE = new IElementType("BRACE_CLOSE", CypherLanguage.INSTANCE);
    IElementType COMMA = new IElementType("COMMA", CypherLanguage.INSTANCE);
    IElementType DOT = new IElementType("DOT", CypherLanguage.INSTANCE);
    IElementType COLON = new IElementType("COLON", CypherLanguage.INSTANCE);
    IElementType SEMICOLON = new IElementType("SEMICOLON", CypherLanguage.INSTANCE);
    IElementType OPERATOR = new IElementType("OPERATOR", CypherLanguage.INSTANCE);
    IElementType PARAMETER = new IElementType("PARAMETER", CypherLanguage.INSTANCE);
}
