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
import java.util.LinkedHashSet;
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
    private static final Set<String> VALUE_KEYWORDS = Set.of(
            "RETURN", "WITH", "WHERE", "ORDER", "BY", "SET", "REMOVE",
            "DELETE", "DETACH", "UNWIND", "FOREACH", "YIELD"
    );
    private static final Set<String> CLAUSE_BOUNDARY_KEYWORDS = Set.of("UNION", "CALL");

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

                        if (isValueContext(position)) {
                            for (String identifier : collectVisibleIdentifiers(position)) {
                                result.addElement(LookupElementBuilder.create(identifier));
                            }
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

    /**
     * Determines whether completion is invoked in a clause that consumes values (RETURN/WITH/WHERE/etc.).
     * Walks backwards until it finds a clause keyword or hits a statement boundary.
     */
    private static boolean isValueContext(@NotNull PsiElement position) {
        PsiElement current = PsiTreeUtil.prevVisibleLeaf(position);
        while (current != null && current.getNode() != null) {
            IElementType type = current.getNode().getElementType();
            if (type == CypherTokenTypes.KEYWORD) {
                String keyword = current.getText().toUpperCase(Locale.ENGLISH);
                if (VALUE_KEYWORDS.contains(keyword)) {
                    return true;
                }
                if (CLAUSE_BOUNDARY_KEYWORDS.contains(keyword)) {
                    return false;
                }
            }
            if (type == CypherTokenTypes.SEMICOLON) {
                return false;
            }
            current = PsiTreeUtil.prevVisibleLeaf(current);
        }
        return false;
    }

    /**
     * Collects identifiers that are likely to be in scope for value clauses by scanning backwards until
     * a statement boundary. Brace blocks are skipped when the caret sits outside them to avoid leaking
     * subquery-local identifiers.
     */
    private static List<String> collectVisibleIdentifiers(@NotNull PsiElement position) {
        LinkedHashSet<String> identifiers = new LinkedHashSet<>();
        PsiElement current = PsiTreeUtil.prevVisibleLeaf(position);
        while (current != null && current.getNode() != null) {
            IElementType type = current.getNode().getElementType();
            if (type == CypherTokenTypes.BRACE_CLOSE) {
                current = skipBraceSection(current);
                continue;
            }
            if (type == CypherTokenTypes.SEMICOLON) {
                break;
            }
            if (type == CypherTokenTypes.KEYWORD) {
                String keyword = current.getText().toUpperCase(Locale.ENGLISH);
                if (CLAUSE_BOUNDARY_KEYWORDS.contains(keyword)) {
                    break;
                }
            }
            if (isValueIdentifier(current)) {
                identifiers.add(current.getText());
            }
            current = PsiTreeUtil.prevVisibleLeaf(current);
        }
        return List.copyOf(identifiers);
    }

    /**
     * Steps back to the token before a balanced {...} block. Used to ignore subquery/property maps when
     * completion is triggered outside of them.
     */
    @Nullable
    private static PsiElement skipBraceSection(@NotNull PsiElement closingBrace) {
        int balance = 1;
        PsiElement current = PsiTreeUtil.prevLeaf(closingBrace);
        while (current != null) {
            if (current.getNode() != null) {
                IElementType type = current.getNode().getElementType();
                if (type == CypherTokenTypes.BRACE_CLOSE) {
                    balance++;
                } else if (type == CypherTokenTypes.BRACE_OPEN) {
                    balance--;
                    if (balance == 0) {
                        return PsiTreeUtil.prevVisibleLeaf(current);
                    }
                }
            }
            current = PsiTreeUtil.prevLeaf(current);
        }
        return current;
    }

    /**
     * Heuristic for variable-like identifiers: skips labels (preceded by colon/dot) and property keys inside maps.
     */
    private static boolean isValueIdentifier(@NotNull PsiElement element) {
        if (element.getNode() == null || element.getNode().getElementType() != CypherTokenTypes.IDENTIFIER) {
            return false;
        }
        PsiElement previous = PsiTreeUtil.prevVisibleLeaf(element);
        if (previous != null && previous.getNode() != null) {
            IElementType type = previous.getNode().getElementType();
            if (type == CypherTokenTypes.COLON || type == CypherTokenTypes.DOT) {
                return false;
            }
        }

        PsiElement next = PsiTreeUtil.nextVisibleLeaf(element);
        if (next != null && next.getNode() != null && next.getNode().getElementType() == CypherTokenTypes.COLON) {
            PsiElement brace = findNearestUnclosedOpening(element, CypherTokenTypes.BRACE_OPEN, CypherTokenTypes.BRACE_CLOSE);
            return brace == null || isSubqueryBrace(brace);
        }
        return true;
    }

    private static boolean isSubqueryBrace(@NotNull PsiElement braceOpen) {
        PsiElement beforeBrace = PsiTreeUtil.prevVisibleLeaf(braceOpen);
        if (beforeBrace == null || beforeBrace.getNode() == null) {
            return false;
        }
        if (beforeBrace.getNode().getElementType() != CypherTokenTypes.KEYWORD) {
            return false;
        }
        return "CALL".equalsIgnoreCase(beforeBrace.getText());
    }
}
