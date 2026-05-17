package com.lindefors.neo4j.cypher;

import com.intellij.formatting.Indent;

/**
 * Centralizes commonly used indent types so they can be reused without reallocation.
 */
final class CypherIndents {
    private CypherIndents() {
    }

    static Indent none() {
        return Indent.getNoneIndent();
    }

    static Indent normal() {
        return Indent.getNormalIndent();
    }

    static Indent continuationWithoutFirst() {
        return Indent.getContinuationWithoutFirstIndent();
    }
}
