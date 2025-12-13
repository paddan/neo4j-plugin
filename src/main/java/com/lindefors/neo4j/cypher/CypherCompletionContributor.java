package com.lindefors.neo4j.cypher;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Provides lightweight Cypher completions (keywords/operators) while avoiding noisy suggestions
 * inside structural graph patterns such as nodes {@code (n:Label)} or relationships {@code -[r:TYPE]->}.
 * The contributor relies on token-level heuristics instead of a full parse tree, so the checks favor
 * fast bail-outs over exhaustive accuracy.
 */
public class CypherCompletionContributor extends CompletionContributor {
    private static final List<String> OPERATORS = Arrays.asList(
            "=", "<>", "<", ">", "<=", ">=", "+", "-", "*", "/", "%", "^",
            "AND", "OR", "XOR", "NOT", "IN", "IS", "CONTAINS", "STARTS", "ENDS"
    );
    private static final Set<String> NODE_PATTERN_KEYWORDS = Set.of("MATCH", "MERGE", "CREATE", "OPTIONAL");

    public CypherCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(CypherLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiElement parent = position.getParent();
                        if (isInCommentOrString(position) || isInCommentOrString(parent)) {
                            return;
                        }
                        if (isInsideNodeOrRelationshipPattern(position)) {
                            return;
                        }

                        // Add keywords
                        for (String keyword : CypherTokenTypes.KEYWORDS) {
                            result.addElement(LookupElementBuilder.create(keyword)
                                    .withCaseSensitivity(false));
                        }

                        // Add operators
                        for (String operator : OPERATORS) {
                            result.addElement(LookupElementBuilder.create(operator)
                                    .withCaseSensitivity(false));
                        }
                    }
                });
    }

    /**
     * Returns true when the element belongs to a comment or string literal token.
     */
    private static boolean isInCommentOrString(@Nullable PsiElement element) {
        if (element == null || element.getNode() == null) {
            return false;
        }
        return element.getNode().getElementType() == CypherTokenTypes.COMMENT
                || element.getNode().getElementType() == CypherTokenTypes.STRING;
    }

    /**
     * Detects whether the caret is currently inside a node pattern {@code (...)} or relationship pattern {@code -[...] -},
     * where offering keyword/operator completions would be intrusive.
     */
    private static boolean isInsideNodeOrRelationshipPattern(@NotNull PsiElement position) {
        return isInsideNodePattern(position) || isInsideRelationshipPattern(position);
    }

    /**
     * Roughly determines if the caret sits within {@code (...)} that is part of a graph pattern.
     * The heuristic looks for pattern-introducing keywords, commas/parentheses, labels, or relationship connectors.
     */
    private static boolean isInsideNodePattern(@NotNull PsiElement position) {
        PsiElement openingParen = findNearestUnclosedOpening(position, CypherTokenTypes.PAREN_OPEN, CypherTokenTypes.PAREN_CLOSE);
        if (openingParen == null) {
            return false;
        }

        if (hasNodePatternPrefix(openingParen) || containsNodeLabel(openingParen)) {
            return true;
        }

        PsiElement closingParen = findMatchingClosing(openingParen, CypherTokenTypes.PAREN_OPEN, CypherTokenTypes.PAREN_CLOSE);
        if (closingParen == null) {
            return hasNodePatternPrefix(openingParen);
        }

        PsiElement afterClose = PsiTreeUtil.nextVisibleLeaf(closingParen);
        return isDashOperator(afterClose);
    }

    /**
     * Checks whether the caret is within {@code [...] } that is surrounded by relationship dashes.
     */
    private static boolean isInsideRelationshipPattern(@NotNull PsiElement position) {
        PsiElement openingBracket = findNearestUnclosedOpening(position, CypherTokenTypes.BRACKET_OPEN, CypherTokenTypes.BRACKET_CLOSE);
        if (openingBracket == null) {
            return false;
        }

        PsiElement beforeBracket = PsiTreeUtil.prevVisibleLeaf(openingBracket);
        if (!isDashOperator(beforeBracket)) {
            return false;
        }

        PsiElement closingBracket = findMatchingClosing(openingBracket, CypherTokenTypes.BRACKET_OPEN, CypherTokenTypes.BRACKET_CLOSE);
        if (closingBracket == null) {
            return true;
        }

        PsiElement afterBracket = PsiTreeUtil.nextVisibleLeaf(closingBracket);
        return isDashOperator(afterBracket);
    }

    /**
     * Determines whether the token before the opening parenthesis is something that typically precedes a node pattern.
     */
    private static boolean hasNodePatternPrefix(@NotNull PsiElement openingParen) {
        PsiElement previous = PsiTreeUtil.prevVisibleLeaf(openingParen);
        if (previous == null) {
            return true;
        }

        IElementType type = previous.getNode().getElementType();
        if (type == CypherTokenTypes.COMMA || type == CypherTokenTypes.PAREN_OPEN) {
            return true;
        }

        if (type == CypherTokenTypes.KEYWORD) {
            String keyword = previous.getText().toUpperCase(Locale.ENGLISH);
            if (NODE_PATTERN_KEYWORDS.contains(keyword)) {
                return true;
            }
        }

        return isDashOperator(previous);
    }

    /**
     * Scans forward from {@code (} to see if a label colon appears before the closing brace or a property map.
     * This helps catch partial patterns like {@code (f:Fr}.
     */
    private static boolean containsNodeLabel(@NotNull PsiElement openingParen) {
        PsiElement current = PsiTreeUtil.nextLeaf(openingParen);
        while (current != null) {
            IElementType type = current.getNode().getElementType();
            if (type == CypherTokenTypes.PAREN_CLOSE) {
                return false;
            }
            if (type == CypherTokenTypes.COLON) {
                return true;
            }
            if (type == CypherTokenTypes.BRACE_OPEN) {
                return false;
            }
            current = PsiTreeUtil.nextLeaf(current);
        }
        return false;
    }

    /**
     * Walks backward from the caret to locate the closest unmatched opening token of the given type.
     */
    private static PsiElement findNearestUnclosedOpening(@NotNull PsiElement start,
                                                         @NotNull IElementType openingType,
                                                         @NotNull IElementType closingType) {
        int balance = 0;
        PsiElement current = start;
        while (current != null) {
            IElementType type = current.getNode().getElementType();
            if (type == closingType) {
                balance++;
            } else if (type == openingType) {
                if (balance == 0) {
                    return current;
                }
                balance--;
            }
            current = PsiTreeUtil.prevLeaf(current);
        }
        return null;
    }

    /**
     * Walks forward from an opening token to find its matching closing token, ignoring nested pairs.
     */
    private static PsiElement findMatchingClosing(@NotNull PsiElement opening,
                                                  @NotNull IElementType openingType,
                                                  @NotNull IElementType closingType) {
        int balance = 0;
        PsiElement current = PsiTreeUtil.nextLeaf(opening);
        while (current != null) {
            IElementType type = current.getNode().getElementType();
            if (type == openingType) {
                balance++;
            } else if (type == closingType) {
                if (balance == 0) {
                    return current;
                }
                balance--;
            }
            current = PsiTreeUtil.nextLeaf(current);
        }
        return null;
    }

    /**
     * Returns true when the element represents a dash-like operator used in relationship patterns.
     */
    private static boolean isDashOperator(@Nullable PsiElement element) {
        if (element == null || element.getNode() == null) {
            return false;
        }
        if (element.getNode().getElementType() != CypherTokenTypes.OPERATOR) {
            return false;
        }
        return element.getText().contains("-");
    }
}
