package com.lindefors.neo4j.cypher;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * Enables Ctrl+/ (line comment) and Ctrl+Shift+/ (block comment) for Cypher files.
 */
public class CypherCommenter implements Commenter {

    @Override
    public @Nullable String getLineCommentPrefix() {
        return "//";
    }

    @Override
    public @Nullable String getBlockCommentPrefix() {
        return "/*";
    }

    @Override
    public @Nullable String getBlockCommentSuffix() {
        return "*/";
    }

    @Override
    public @Nullable String getCommentedBlockCommentPrefix() {
        return null;
    }

    @Override
    public @Nullable String getCommentedBlockCommentSuffix() {
        return null;
    }
}
