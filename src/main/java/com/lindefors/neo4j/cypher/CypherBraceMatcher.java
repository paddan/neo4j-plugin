package com.lindefors.neo4j.cypher;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CypherBraceMatcher implements PairedBraceMatcher {
    private static final BracePair[] PAIRS = new BracePair[]{
            new BracePair(CypherTokenTypes.PAREN_OPEN, CypherTokenTypes.PAREN_CLOSE, false),
            new BracePair(CypherTokenTypes.BRACKET_OPEN, CypherTokenTypes.BRACKET_CLOSE, false),
            new BracePair(CypherTokenTypes.BRACE_OPEN, CypherTokenTypes.BRACE_CLOSE, true)
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType,
                                                   @Nullable IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(@NotNull PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
