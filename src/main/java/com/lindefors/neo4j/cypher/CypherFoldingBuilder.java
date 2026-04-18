package com.lindefors.neo4j.cypher;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Provides folding for Cypher files by pairing parentheses, brackets, and braces when their contents
 * span multiple lines, and by collapsing multi-line block comments.
 *
 * <p>The AST walk is iterative (using an explicit stack) to avoid stack-overflow on deeply nested input.
 */
public class CypherFoldingBuilder implements FoldingBuilder {

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull ASTNode node,
                                                          @NotNull Document document) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        Deque<ASTNode> delimiterStack = new ArrayDeque<>();

        // Iterative pre-order walk using an explicit node stack
        Deque<ASTNode> nodeStack = new ArrayDeque<>();
        nodeStack.push(node);

        while (!nodeStack.isEmpty()) {
            ASTNode current = nodeStack.pop();
            IElementType type = current.getElementType();

            // Block comment spanning multiple lines is foldable
            if (type == CypherTokenTypes.COMMENT) {
                String text = current.getText();
                if (text.startsWith("/*")) {
                    TextRange range = current.getTextRange();
                    if (document.getLineNumber(range.getStartOffset()) < document.getLineNumber(range.getEndOffset())) {
                        descriptors.add(new FoldingDescriptor(current, range));
                    }
                }
            }

            if (isOpening(type)) {
                delimiterStack.push(current);
            } else if (isClosing(type) && !delimiterStack.isEmpty()) {
                ASTNode opening = delimiterStack.peek();
                if (matches(opening.getElementType(), type)) {
                    delimiterStack.pop();
                    if (isMultiline(document, opening, current)) {
                        TextRange range = new TextRange(opening.getTextRange().getStartOffset(),
                                current.getTextRange().getEndOffset());
                        descriptors.add(new FoldingDescriptor(opening, range));
                    }
                }
            }

            // Push children in reverse order so the first child is processed first
            List<ASTNode> children = new ArrayList<>();
            for (ASTNode child = current.getFirstChildNode(); child != null; child = child.getTreeNext()) {
                children.add(child);
            }
            for (int i = children.size() - 1; i >= 0; i--) {
                nodeStack.push(children.get(i));
            }
        }

        return descriptors.toArray(FoldingDescriptor[]::new);
    }

    @Override
    public @NotNull String getPlaceholderText(@NotNull ASTNode node) {
        IElementType type = node.getElementType();
        if (type == CypherTokenTypes.COMMENT) {
            return "/* ... */";
        }
        if (type == CypherTokenTypes.BRACE_OPEN) {
            return "{...}";
        }
        if (type == CypherTokenTypes.BRACKET_OPEN) {
            return "[...]";
        }
        if (type == CypherTokenTypes.PAREN_OPEN) {
            return "(...)";
        }
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }

    private boolean isOpening(IElementType type) {
        return type == CypherTokenTypes.BRACE_OPEN
                || type == CypherTokenTypes.BRACKET_OPEN
                || type == CypherTokenTypes.PAREN_OPEN;
    }

    private boolean isClosing(IElementType type) {
        return type == CypherTokenTypes.BRACE_CLOSE
                || type == CypherTokenTypes.BRACKET_CLOSE
                || type == CypherTokenTypes.PAREN_CLOSE;
    }

    private boolean matches(IElementType opening, IElementType closing) {
        return (opening == CypherTokenTypes.BRACE_OPEN && closing == CypherTokenTypes.BRACE_CLOSE)
                || (opening == CypherTokenTypes.BRACKET_OPEN && closing == CypherTokenTypes.BRACKET_CLOSE)
                || (opening == CypherTokenTypes.PAREN_OPEN && closing == CypherTokenTypes.PAREN_CLOSE);
    }

    private boolean isMultiline(Document document, ASTNode opening, ASTNode closing) {
        int startLine = document.getLineNumber(opening.getTextRange().getStartOffset());
        int endLine = document.getLineNumber(closing.getTextRange().getEndOffset());
        return endLine > startLine;
    }
}
