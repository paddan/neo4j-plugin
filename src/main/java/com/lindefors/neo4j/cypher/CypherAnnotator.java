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

/**
 * Applies semantic highlighting (node labels, relationship types, property keys, function names)
 * and flags structural errors (unmatched delimiters, consecutive clause keywords) in a single
 * pass over the flat token list.
 */
public class CypherAnnotator implements Annotator {

    record LeafToken(IElementType type, String text, TextRange range) {}

    private record OpenDelimiter(LeafToken token, IElementType expectedClose) {}

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

    // Works because CypherParser produces a flat tree: all tokens are direct children of the root.
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

        Deque<OpenDelimiter> delimiterStack = new ArrayDeque<>();
        // true = map literal {}, false = a CALL/EXISTS/COUNT/COLLECT subquery block
        Deque<Boolean> braceIsMapStack = new ArrayDeque<>();
        Deque<Boolean> bracketIsRelationshipStack = new ArrayDeque<>();

        String lastClauseKeyword = null;
        boolean seenContentAfterClause = false;

        for (int i = 0; i < tokens.size(); i++) {
            LeafToken tok = tokens.get(i);
            IElementType type = tok.type();

            // Bracket depth tracking (used for both semantic highlighting and error marking)
            if (type == CypherTokenTypes.PAREN_OPEN) {
                delimiterStack.push(new OpenDelimiter(tok, CypherTokenTypes.PAREN_CLOSE));
            } else if (type == CypherTokenTypes.BRACKET_OPEN) {
                delimiterStack.push(new OpenDelimiter(tok, CypherTokenTypes.BRACKET_CLOSE));
                bracketIsRelationshipStack.push(CypherTokenContext.isRelationshipPatternBracket(
                        tokens, i, LeafToken::type, LeafToken::text));
            } else if (type == CypherTokenTypes.BRACE_OPEN) {
                delimiterStack.push(new OpenDelimiter(tok, CypherTokenTypes.BRACE_CLOSE));
                boolean isSubquery = CypherTokenContext.isSubqueryBrace(tokens, i, LeafToken::type, LeafToken::text);
                braceIsMapStack.push(!isSubquery);
            } else if (isClosingDelimiter(type)) {
                if (delimiterStack.isEmpty() || delimiterStack.peek().expectedClose() != type) {
                    result.add(Annotation.error(tok.range(), "Unmatched '" + tok.text() + "'"));
                } else {
                    delimiterStack.pop();
                    if (type == CypherTokenTypes.BRACE_CLOSE) {
                        braceIsMapStack.pop();
                    } else if (type == CypherTokenTypes.BRACKET_CLOSE) {
                        bracketIsRelationshipStack.pop();
                    }
                }
            }

            // Unterminated literals (strings, backtick identifiers, block comments, $(...) parameters)
            if (type == CypherTokenTypes.STRING
                    || type == CypherTokenTypes.IDENTIFIER
                    || type == CypherTokenTypes.COMMENT
                    || type == CypherTokenTypes.PARAMETER) {
                String unterminatedMessage = unterminatedMessage(tok);
                if (unterminatedMessage != null) {
                    result.add(Annotation.error(tok.range(), unterminatedMessage));
                }
            }

            // Semantic highlighting
            if (type == CypherTokenTypes.IDENTIFIER) {
                LeafToken next = nextSignificant(tokens, i);

                boolean insideMapBrace = !braceIsMapStack.isEmpty() && braceIsMapStack.peek();
                if (!insideMapBrace && isInLabelContext(tokens, i)) {
                    if (!bracketIsRelationshipStack.isEmpty() && bracketIsRelationshipStack.peek()) {
                        result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.RELATIONSHIP_TYPE));
                    } else {
                        result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.LABEL));
                    }
                } else if (next != null && next.type() == CypherTokenTypes.PAREN_OPEN) {
                    result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.FUNCTION_NAME));
                } else if (next != null && next.type() == CypherTokenTypes.COLON && insideMapBrace) {
                    result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.PROPERTY_KEY));
                }
            }

            // Consecutive clause keyword detection
            if (type == CypherTokenTypes.KEYWORD) {
                String upper = tok.text().toUpperCase(Locale.ENGLISH);
                if (CypherTokenTypes.CLAUSE_START_KEYWORDS.contains(upper)) {
                    if (lastClauseKeyword != null && !seenContentAfterClause
                            && !CypherTokenContext.isCompoundClauseContinuation(lastClauseKeyword, upper)
                            && !isMergeActionSet(tokens, i, upper)) {
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
        for (OpenDelimiter delimiter : delimiterStack) {
            LeafToken tok = delimiter.token();
            result.add(Annotation.error(tok.range(), "Unmatched '" + tok.text() + "'"));
        }

        return result;
    }

    private static boolean isMergeActionSet(List<LeafToken> tokens, int index, String upper) {
        if (!"SET".equals(upper)) return false;

        int actionIndex = prevSignificantIndex(tokens, index);
        if (actionIndex < 0) return false;
        String action = tokens.get(actionIndex).text().toUpperCase(Locale.ENGLISH);
        if (!"CREATE".equals(action) && !"MATCH".equals(action)) return false;

        int onIndex = prevSignificantIndex(tokens, actionIndex);
        return onIndex >= 0 && "ON".equalsIgnoreCase(tokens.get(onIndex).text());
    }

    @Nullable
    private static String unterminatedMessage(LeafToken tok) {
        IElementType type = tok.type();
        String text = tok.text();
        if (text == null || text.isEmpty()) return null;

        if (type == CypherTokenTypes.STRING) {
            char open = text.charAt(0);
            if (open != '\'' && open != '"') return null;
            if (text.length() < 2 || text.charAt(text.length() - 1) != open) {
                return "Unterminated string literal";
            }
            return null;
        }
        if (type == CypherTokenTypes.IDENTIFIER && text.charAt(0) == '`') {
            if (text.length() < 2 || text.charAt(text.length() - 1) != '`') {
                return "Unterminated quoted identifier";
            }
            return null;
        }
        if (type == CypherTokenTypes.COMMENT && text.startsWith("/*")) {
            // length < 4 catches the degenerate "/*/" case where endsWith("*/") is true
            // but the comment is still unterminated (the third '/' is part of the opener).
            if (!text.endsWith("*/") || text.length() < 4) {
                return "Unterminated block comment";
            }
        }
        if (type == CypherTokenTypes.PARAMETER && text.length() >= 2
                && text.charAt(0) == '$' && text.charAt(1) == '(') {
            if (text.charAt(text.length() - 1) != ')') {
                return "Unterminated parameter expression";
            }
        }
        return null;
    }

    // Returns true if tokens[i] is in a label/reltype position: directly after :,
    // or after a | pipe chain that originates from a :.  E.g. :Movie|Actor|Director.
    private boolean isInLabelContext(List<LeafToken> tokens, int i) {
        int j = i - 1;
        while (j >= 0) {
            LeafToken t = tokens.get(j);
            if (t.type() == TokenType.WHITE_SPACE) { j--; continue; }
            if (t.type() == CypherTokenTypes.COLON) return true;
            if (t.type() == CypherTokenTypes.OPERATOR && "|".equals(t.text())) {
                j--;
                while (j >= 0 && tokens.get(j).type() == TokenType.WHITE_SPACE) j--;
                if (j < 0 || tokens.get(j).type() != CypherTokenTypes.IDENTIFIER) return false;
                j--;
                continue;
            }
            return false;
        }
        return false;
    }

    private LeafToken nextSignificant(List<LeafToken> tokens, int i) {
        for (int j = i + 1; j < tokens.size(); j++) {
            if (tokens.get(j).type() != TokenType.WHITE_SPACE) return tokens.get(j);
        }
        return null;
    }

    private static int prevSignificantIndex(List<LeafToken> tokens, int i) {
        for (int j = i - 1; j >= 0; j--) {
            if (tokens.get(j).type() != TokenType.WHITE_SPACE) return j;
        }
        return -1;
    }

    private boolean isClosingDelimiter(IElementType type) {
        return type == CypherTokenTypes.PAREN_CLOSE
                || type == CypherTokenTypes.BRACKET_CLOSE
                || type == CypherTokenTypes.BRACE_CLOSE;
    }

}
