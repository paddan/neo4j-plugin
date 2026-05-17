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
            "ALTER", "DROP",

            // Conditional / sequential queries
            "CASE", "WHEN", "THEN", "ELSE", "END", "NEXT", "FINISH",

            // Subquery batching
            "IN", "TRANSACTION", "TRANSACTIONS", "CONCURRENT", "OF", "ROWS", "ROW",
            "ON", "ERROR", "CONTINUE", "RETRY", "STATUS", "REPORT",

            // Predicates and operators
            "AND", "OR", "XOR", "NOT", "IS", "STARTS", "ENDS", "CONTAINS",
            "TRUE", "FALSE", "NULL", "COUNT",
            "NORMALIZED", "NFC", "NFD", "NFKC", "NFKD",
            "TYPED", "DIFFERENT",

            // Pattern options and path finding
            "SHORTEST", "ANY", "GROUPS", "REPEATABLE", "ELEMENTS",

            // Schema / indexes / constraints
            "INDEX", "INDEXES", "RANGE", "TEXT", "POINT", "LOOKUP", "VECTOR", "FULLTEXT",
            "OPTIONS", "EACH", "FOR", "USING",
            "CONSTRAINT", "CONSTRAINTS", "REQUIRE", "UNIQUE", "KEY",
            "NODE", "RELATIONSHIP",
            "SEEK", "SCAN", "JOIN", "PERIODIC", "BOOSTED",

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
            "TO", "AT",
            "WAIT", "NOWAIT", "BRIEF", "VERBOSE"
    )));

    /**
     * Keywords that are also built-in functions. The lexer uses one-character lookahead: when one of
     * these names is immediately followed by {@code (}, it is emitted as {@link #IDENTIFIER} so that
     * function-call syntax gets function highlighting/completion rather than keyword treatment.
     */
    public static final Set<String> KEYWORD_FUNCTIONS = Set.of(
            "ALL", "ANY", "COUNT", "EXISTS", "POINT", "RANGE", "REPLACE"
    );

    /**
     * Keywords that begin a new Cypher clause and should be placed on their own line by the formatter.
     * Shared by the formatter and completion contributor to avoid duplication.
     */
    public static final Set<String> CLAUSE_START_KEYWORDS = Set.of(
            "ALTER",
            "CALL",
            "CREATE",
            "DELETE",
            "DENY",
            "DETACH",
            "DROP",
            "FINISH",
            "FOREACH",
            "GRANT",
            "LET",
            "LOAD",
            "MATCH",
            "MERGE",
            "NEXT",
            "OPTIONAL",
            "RETURN",
            "REMOVE",
            "REVOKE",
            "SET",
            "SHOW",
            "START",
            "STOP",
            "TERMINATE",
            "UNION",
            "UNWIND",
            "USE",
            "WHERE",
            "WITH"
    );

    /**
     * Keywords that continue a clause (same logical unit) but follow on a new line.
     * Shared by the formatter and completion contributor to avoid duplication.
     */
    public static final Set<String> CLAUSE_CONTINUATION_KEYWORDS = Set.of(
            "BY",
            "LIMIT",
            "ON",
            "ORDER",
            "SKIP",
            "THEN",
            "YIELD"
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
