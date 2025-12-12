package com.lindefors.neo4j.cypher;

import com.intellij.lang.Language;

/**
 * Identifies the Cypher language within IntelliJ's platform so file types and language-level services
 * can be registered against it.
 */
public class CypherLanguage extends Language {
    public static final CypherLanguage INSTANCE = new CypherLanguage();

    private CypherLanguage() {
        super("Cypher");
    }
}
