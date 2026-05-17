package com.lindefors.neo4j.cypher;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Applies semantic highlighting (node labels, relationship types, property keys, function names)
 * and flags structural errors (unmatched delimiters, consecutive clause keywords) in a single
 * pass over the flat token list.
 */
public class CypherAnnotator implements Annotator {

    private static final Set<String> VALID_CONSECUTIVE_PAIRS = Set.of(
            "OPTIONAL:MATCH",
            "DETACH:DELETE"
    );

    record LeafToken(IElementType type, String text, TextRange range) {}

    record Annotation(
            TextRange range,
            HighlightSeverity severity,
            @Nullable String message,
            @Nullable TextAttributesKey attributes) {

        static Annotation highlight(TextRange range, TextAttributesKey key) {
            return new Annotation(range, HighlightSeverity.INFORMATION, null, key);
        }

        static Annotation error(TextRange range, String message) {
            return new Annotation(range, HighlightSeverity.ERROR, message, null);
        }

        static Annotation warning(TextRange range, String message) {
            return new Annotation(range, HighlightSeverity.WARNING, message, null);
        }
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof CypherPsiFile)) return;

        for (Annotation ann : computeAnnotations(collectTokens(element))) {
            if (ann.attributes() != null) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(ann.range())
                        .textAttributes(ann.attributes())
                        .create();
            } else {
                holder.newAnnotation(ann.severity(), ann.message())
                        .range(ann.range())
                        .create();
            }
        }
    }

    private List<LeafToken> collectTokens(PsiElement root) {
        List<LeafToken> result = new ArrayList<>();
        PsiElement child = root.getFirstChild();
        while (child != null) {
            result.add(new LeafToken(
                    child.getNode().getElementType(),
                    child.getText(),
                    child.getTextRange()));
            child = child.getNextSibling();
        }
        return result;
    }

    List<Annotation> computeAnnotations(List<LeafToken> tokens) {
        List<Annotation> result = new ArrayList<>();

        Deque<LeafToken> parenStack = new ArrayDeque<>();
        Deque<LeafToken> bracketStack = new ArrayDeque<>();
        Deque<LeafToken> braceStack = new ArrayDeque<>();

        String lastClauseKeyword = null;
        boolean seenContentAfterClause = false;

        for (int i = 0; i < tokens.size(); i++) {
            LeafToken tok = tokens.get(i);
            IElementType type = tok.type();

            // Bracket depth tracking (used for both semantic highlighting and error marking)
            if (type == CypherTokenTypes.PAREN_OPEN) {
                parenStack.push(tok);
            } else if (type == CypherTokenTypes.BRACKET_OPEN) {
                bracketStack.push(tok);
            } else if (type == CypherTokenTypes.BRACE_OPEN) {
                braceStack.push(tok);
            } else if (type == CypherTokenTypes.PAREN_CLOSE) {
                if (parenStack.isEmpty()) result.add(Annotation.error(tok.range(), "Unmatched ')'"));
                else parenStack.pop();
            } else if (type == CypherTokenTypes.BRACKET_CLOSE) {
                if (bracketStack.isEmpty()) result.add(Annotation.error(tok.range(), "Unmatched ']'"));
                else bracketStack.pop();
            } else if (type == CypherTokenTypes.BRACE_CLOSE) {
                if (braceStack.isEmpty()) result.add(Annotation.error(tok.range(), "Unmatched '}'"));
                else braceStack.pop();
            }

            // Semantic highlighting
            if (type == CypherTokenTypes.IDENTIFIER) {
                LeafToken prev = prevSignificant(tokens, i);
                LeafToken next = nextSignificant(tokens, i);

                if (prev != null && prev.type() == CypherTokenTypes.COLON) {
                    if (!bracketStack.isEmpty()) {
                        result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.RELATIONSHIP_TYPE));
                    } else {
                        result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.LABEL));
                    }
                } else if (next != null && next.type() == CypherTokenTypes.PAREN_OPEN) {
                    result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.FUNCTION_NAME));
                } else if (next != null && next.type() == CypherTokenTypes.COLON && !braceStack.isEmpty()) {
                    result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.PROPERTY_KEY));
                }
            }

            // Consecutive clause keyword detection
            if (type == CypherTokenTypes.KEYWORD) {
                String upper = tok.text().toUpperCase(Locale.ENGLISH);
                if (CypherTokenTypes.CLAUSE_START_KEYWORDS.contains(upper)) {
                    if (lastClauseKeyword != null && !seenContentAfterClause
                            && !VALID_CONSECUTIVE_PAIRS.contains(lastClauseKeyword + ":" + upper)) {
                        result.add(Annotation.warning(tok.range(), "Unexpected keyword, missing clause body"));
                    }
                    lastClauseKeyword = upper;
                    seenContentAfterClause = false;
                } else {
                    seenContentAfterClause = true;
                }
            } else if (type != TokenType.WHITE_SPACE) {
                seenContentAfterClause = true;
            }
        }

        // Unclosed delimiters
        for (LeafToken tok : parenStack) result.add(Annotation.error(tok.range(), "Unmatched '('"));
        for (LeafToken tok : bracketStack) result.add(Annotation.error(tok.range(), "Unmatched '['"));
        for (LeafToken tok : braceStack) result.add(Annotation.error(tok.range(), "Unmatched '{'"));

        return result;
    }

    private LeafToken prevSignificant(List<LeafToken> tokens, int i) {
        for (int j = i - 1; j >= 0; j--) {
            if (tokens.get(j).type() != TokenType.WHITE_SPACE) return tokens.get(j);
        }
        return null;
    }

    private LeafToken nextSignificant(List<LeafToken> tokens, int i) {
        for (int j = i + 1; j < tokens.size(); j++) {
            if (tokens.get(j).type() != TokenType.WHITE_SPACE) return tokens.get(j);
        }
        return null;
    }
}
