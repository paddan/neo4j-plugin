package com.lindefors.neo4j.cypher;

import com.intellij.psi.tree.IElementType;

import java.util.Set;

/**
 * Defines the token types produced by {@link CypherLexer} and reused throughout highlighting, formatting,
 * and completion logic.
 */
public final class CypherTokenTypes {
    private CypherTokenTypes() {
    }

    /**
     * Upper-case Cypher keywords recognized by the lexer.
     *
     * <p>Keep this in sync with keyword-based editor features (completion and formatter heuristics).
     */
    public static final Set<String> KEYWORDS = Set.of(
            // Core clauses
            "MATCH", "OPTIONAL", "WHERE", "RETURN", "WITH", "UNWIND",
            "CREATE", "MERGE", "DELETE", "DETACH", "SET", "REMOVE",
            "FOREACH", "LOAD", "CSV", "FROM", "HEADERS", "CALL", "YIELD",
            "USE",

            // Projection / ordering
            "AS", "ORDER", "BY", "SKIP", "LIMIT", "ASC", "DESC",
            "UNION", "ALL", "DISTINCT",

            // Schema / planning / misc
            "ON", "USING", "INDEX", "CONSTRAINT", "EXISTS",
            "PROFILE", "EXPLAIN",
            "SHOW", "TERMINATE",

            // Literals / functions (limited)
            "TRUE", "FALSE", "NULL", "COUNT"
    );

    public static final IElementType KEYWORD = new IElementType("KEYWORD", CypherLanguage.INSTANCE);
    public static final IElementType IDENTIFIER = new IElementType("IDENTIFIER", CypherLanguage.INSTANCE);
    public static final IElementType NUMBER = new IElementType("NUMBER", CypherLanguage.INSTANCE);
    public static final IElementType STRING = new IElementType("STRING", CypherLanguage.INSTANCE);
    public static final IElementType COMMENT = new IElementType("COMMENT", CypherLanguage.INSTANCE);

    public static final IElementType PAREN_OPEN = new IElementType("PAREN_OPEN", CypherLanguage.INSTANCE);
    public static final IElementType PAREN_CLOSE = new IElementType("PAREN_CLOSE", CypherLanguage.INSTANCE);
    public static final IElementType BRACKET_OPEN = new IElementType("BRACKET_OPEN", CypherLanguage.INSTANCE);
    public static final IElementType BRACKET_CLOSE = new IElementType("BRACKET_CLOSE", CypherLanguage.INSTANCE);
    public static final IElementType BRACE_OPEN = new IElementType("BRACE_OPEN", CypherLanguage.INSTANCE);
    public static final IElementType BRACE_CLOSE = new IElementType("BRACE_CLOSE", CypherLanguage.INSTANCE);

    public static final IElementType COMMA = new IElementType("COMMA", CypherLanguage.INSTANCE);
    public static final IElementType DOT = new IElementType("DOT", CypherLanguage.INSTANCE);
    public static final IElementType COLON = new IElementType("COLON", CypherLanguage.INSTANCE);
    public static final IElementType SEMICOLON = new IElementType("SEMICOLON", CypherLanguage.INSTANCE);

    public static final IElementType OPERATOR = new IElementType("OPERATOR", CypherLanguage.INSTANCE);
    public static final IElementType PARAMETER = new IElementType("PARAMETER", CypherLanguage.INSTANCE);
}
