package com.lindefors.neo4j.cypher;

import com.intellij.lang.ASTNode;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * Shared structural heuristics for the flat Cypher token stream.
 */
final class CypherTokenContext {

    private static final Set<String> COMPOUND_CLAUSES = Set.of(
            "OPTIONAL:MATCH",
            "DETACH:DELETE"
    );

    private CypherTokenContext() {
    }

    static boolean isCompoundClauseContinuation(@Nullable String previousKeyword, @NotNull String keyword) {
        return previousKeyword != null
                && COMPOUND_CLAUSES.contains(previousKeyword.toUpperCase(Locale.ENGLISH)
                + ":" + keyword.toUpperCase(Locale.ENGLISH));
    }

    static boolean isSubqueryBrace(@NotNull ASTNode braceOpen) {
        ASTNode previous = previousSignificant(braceOpen);
        if (previous == null) {
            return false;
        }
        if (previous.getElementType() == CypherTokenTypes.KEYWORD) {
            return CypherTokenTypes.SUBQUERY_KEYWORDS.contains(previous.getText().toUpperCase(Locale.ENGLISH));
        }
        if (previous.getElementType() != CypherTokenTypes.PAREN_CLOSE) {
            return false;
        }

        ASTNode scopeOpen = findMatchingOpening(previous, CypherTokenTypes.PAREN_OPEN, CypherTokenTypes.PAREN_CLOSE);
        ASTNode beforeScope = scopeOpen == null ? null : previousSignificant(scopeOpen);
        return beforeScope != null
                && beforeScope.getElementType() == CypherTokenTypes.KEYWORD
                && "CALL".equalsIgnoreCase(beforeScope.getText());
    }

    static boolean isInsideMapLiteral(@NotNull ASTNode node) {
        ASTNode openingBrace = findNearestOpeningBrace(node);
        return openingBrace != null && !isSubqueryBrace(openingBrace);
    }

    static <T> boolean isSubqueryBrace(
            @NotNull List<T> tokens,
            int braceIndex,
            @NotNull Function<T, IElementType> type,
            @NotNull Function<T, String> text) {
        int previous = previousSignificantIndex(tokens, braceIndex, type);
        if (previous < 0) {
            return false;
        }
        IElementType previousType = type.apply(tokens.get(previous));
        if (previousType == CypherTokenTypes.KEYWORD) {
            return CypherTokenTypes.SUBQUERY_KEYWORDS.contains(
                    text.apply(tokens.get(previous)).toUpperCase(Locale.ENGLISH));
        }
        if (previousType != CypherTokenTypes.PAREN_CLOSE) {
            return false;
        }

        int depth = 1;
        for (int i = previous - 1; i >= 0; i--) {
            IElementType currentType = type.apply(tokens.get(i));
            if (currentType == CypherTokenTypes.PAREN_CLOSE) {
                depth++;
            } else if (currentType == CypherTokenTypes.PAREN_OPEN && --depth == 0) {
                int beforeScope = previousSignificantIndex(tokens, i, type);
                return beforeScope >= 0
                        && type.apply(tokens.get(beforeScope)) == CypherTokenTypes.KEYWORD
                        && "CALL".equalsIgnoreCase(text.apply(tokens.get(beforeScope)));
            }
        }
        return false;
    }

    static <T> boolean isRelationshipPatternBracket(
            @NotNull List<T> tokens,
            int bracketIndex,
            @NotNull Function<T, IElementType> type,
            @NotNull Function<T, String> text) {
        int previous = previousSignificantIndex(tokens, bracketIndex, type);
        return previous >= 0
                && type.apply(tokens.get(previous)) == CypherTokenTypes.OPERATOR
                && text.apply(tokens.get(previous)).contains("-");
    }

    private static @Nullable ASTNode findNearestOpeningBrace(@NotNull ASTNode node) {
        int depth = 0;
        ASTNode current = node.getTreePrev();
        while (current != null) {
            IElementType type = current.getElementType();
            if (type == CypherTokenTypes.BRACE_CLOSE) {
                depth++;
            } else if (type == CypherTokenTypes.BRACE_OPEN) {
                if (depth == 0) {
                    return current;
                }
                depth--;
            }
            current = current.getTreePrev();
        }
        return null;
    }

    private static @Nullable ASTNode findMatchingOpening(
            @NotNull ASTNode closing,
            @NotNull IElementType openingType,
            @NotNull IElementType closingType) {
        int depth = 1;
        ASTNode current = closing.getTreePrev();
        while (current != null) {
            IElementType type = current.getElementType();
            if (type == closingType) {
                depth++;
            } else if (type == openingType && --depth == 0) {
                return current;
            }
            current = current.getTreePrev();
        }
        return null;
    }

    private static @Nullable ASTNode previousSignificant(@NotNull ASTNode node) {
        ASTNode current = node.getTreePrev();
        while (current != null && isInsignificant(current.getElementType())) {
            current = current.getTreePrev();
        }
        return current;
    }

    private static <T> int previousSignificantIndex(
            @NotNull List<T> tokens,
            int index,
            @NotNull Function<T, IElementType> type) {
        for (int i = index - 1; i >= 0; i--) {
            if (!isInsignificant(type.apply(tokens.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isInsignificant(@NotNull IElementType type) {
        return type == TokenType.WHITE_SPACE || type == CypherTokenTypes.COMMENT;
    }
}
