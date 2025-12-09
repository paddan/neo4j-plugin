package com.lindefors.neo4j.cypher;

import com.intellij.formatting.Indent;

final class CypherIndents {
    private static final Indent NONE = Indent.getNoneIndent();
    private static final Indent NORMAL = Indent.getNormalIndent();
    private static final Indent CONTINUATION_WITHOUT_FIRST = Indent.getContinuationWithoutFirstIndent();

    private CypherIndents() {
    }

    static Indent none() {
        return NONE;
    }

    static Indent normal() {
        return NORMAL;
    }

    static Indent continuationWithoutFirst() {
        return CONTINUATION_WITHOUT_FIRST;
    }
}
