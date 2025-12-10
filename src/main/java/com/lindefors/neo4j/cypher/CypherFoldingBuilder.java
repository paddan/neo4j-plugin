package com.lindefors.neo4j.cypher;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class CypherFoldingBuilder extends FoldingBuilderEx {
    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root,
                                                          @NotNull Document document,
                                                          boolean quick) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        Deque<ASTNode> stack = new ArrayDeque<>();

        for (ASTNode node = root.getNode().getFirstChildNode(); node != null; node = node.getTreeNext()) {
            IElementType type = node.getElementType();
            if (isOpening(type)) {
                stack.push(node);
                continue;
            }

            if (isClosing(type) && !stack.isEmpty()) {
                ASTNode opening = stack.peek();
                if (matches(opening.getElementType(), type)) {
                    stack.pop();
                    if (isMultiline(document, opening, node)) {
                        TextRange range = new TextRange(opening.getTextRange().getStartOffset(),
                                node.getTextRange().getEndOffset());
                        descriptors.add(new FoldingDescriptor(opening, range));
                    }
                }
            }
        }

        return descriptors.toArray(FoldingDescriptor[]::new);
    }

    @Override
    public @NotNull String getPlaceholderText(@NotNull ASTNode node) {
        IElementType type = node.getElementType();
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
