package com.lindefors.neo4j.cypher;

import com.intellij.psi.tree.IElementType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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
    public static final Set<String> KEYWORDS = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(
            // Core clauses
            "USE", "MATCH", "OPTIONAL", "WHERE", "FILTER", "RETURN", "WITH", "LET",
            "UNWIND", "CREATE", "MERGE", "DELETE", "DETACH", "SET", "REMOVE",
            "FOREACH", "CALL", "YIELD", "LOAD", "CSV", "HEADERS", "FIELDTERMINATOR", "FROM",
            "ORDER", "BY", "SKIP", "LIMIT", "ASC", "DESC",
            "UNION", "ALL", "DISTINCT", "AS",
            "PROFILE", "EXPLAIN",

            // Conditional / sequential queries
            "CASE", "WHEN", "THEN", "ELSE", "END", "NEXT", "FINISH",

            // Subquery batching
            "IN", "TRANSACTION", "TRANSACTIONS", "CONCURRENT", "OF", "ROWS", "ROW",
            "ON", "ERROR", "CONTINUE", "RETRY", "STATUS", "REPORT",

            // Predicates and operators
            "AND", "OR", "XOR", "NOT", "IS", "STARTS", "ENDS", "CONTAINS",
            "TRUE", "FALSE", "NULL", "EXISTS", "COUNT",
            "NORMALIZED", "NFC", "NFD", "NFKC", "NFKD",

            // Pattern options and path finding
            "SHORTEST", "ANY", "GROUPS", "REPEATABLE", "ELEMENTS",

            // Schema / indexes / constraints
            "INDEX", "INDEXES", "RANGE", "TEXT", "POINT", "LOOKUP", "VECTOR", "FULLTEXT",
            "OPTIONS", "EACH", "FOR", "USING",
            "CONSTRAINT", "CONSTRAINTS", "REQUIRE", "UNIQUE", "KEY",
            "NODE", "RELATIONSHIP",

            // Administrative commands and privileges
            "SHOW", "TERMINATE", "STOP", "START", "GRANT", "DENY", "REVOKE",
            "PRIVILEGE", "PRIVILEGES", "IMMUTABLE", "SUPPORTED",
            "DATABASE", "DATABASES", "DEFAULT", "HOME", "COMPOSITE", "ALIAS", "ALIASES",
            "SERVER", "SERVERS", "USER", "USERS", "ROLE", "ROLES",
            "DBMS", "GRAPH", "DATA", "MANAGEMENT", "ACCESS", "READ", "WRITE", "ONLY",
            "IF", "EXISTS", "REPLACE", "CASCADE", "ENABLE", "REALLOCATE", "DEALLOCATE",
            "PASSWORD", "ACTIVE", "SUSPENDED", "LANGUAGE", "CYPHER", "VERSION",
            "PRIMARY", "SECONDARIES", "TOPOLOGY", "EXECUTE", "COPY", "CURRENT", "COMMANDS",
            "FUNCTIONS", "PROCEDURES", "SETTINGS", "BUILT", "EXECUTABLE", "POPULATED",
            "TO", "FROM", "AT"
    )));

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
