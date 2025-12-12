package com.lindefors.neo4j.cypher;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Trivial parser that produces a flat AST by consuming all tokens. This keeps the plugin lightweight
 * while still enabling editor features that rely on PSI nodes.
 */
public class CypherParser implements PsiParser {
    /**
     * Consumes tokens until EOF and wraps them under the provided root element type.
     */
    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        while (!builder.eof()) {
            builder.advanceLexer();
        }
        marker.done(root);
        return builder.getTreeBuilt();
    }
}
