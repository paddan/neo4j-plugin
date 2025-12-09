package com.lindefors.neo4j.cypher;

import com.intellij.lang.Language;

public class CypherLanguage extends Language {
    public static final CypherLanguage INSTANCE = new CypherLanguage();

    private CypherLanguage() {
        super("Cypher");
    }
}
