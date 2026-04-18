package com.lindefors.neo4j.cypher;

import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Represents either the root Cypher file or a single top-level clause in the Structure panel.
 * The file element collects all clause-starting keyword tokens as children; each clause element
 * is a leaf that shows the keyword plus a short snippet of the following tokens.
 */
public class CypherStructureViewElement implements StructureViewTreeElement {

    private static final int SNIPPET_MAX_CHARS = 60;
    private static final TreeElement[] EMPTY = new TreeElement[0];

    private final PsiElement element;

    public CypherStructureViewElement(@NotNull PsiElement element) {
        this.element = element;
    }

    @Override
    public Object getValue() {
        return element;
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (element instanceof PsiFile) {
            return;
        }
        if (element.isValid()) {
            element.getContainingFile().navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        return !(element instanceof PsiFile) && element.isValid();
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    public @NotNull ItemPresentation getPresentation() {
        if (element instanceof PsiFile) {
            return new ItemPresentation() {
                @Override
                public @NotNull String getPresentableText() {
                    return element.getContainingFile().getName();
                }

                @Override
                public @Nullable Icon getIcon(boolean unused) {
                    return CypherFileType.INSTANCE.getIcon();
                }
            };
        }

        String clauseText = buildClauseSnippet(element);
        return new ItemPresentation() {
            @Override
            public @NotNull String getPresentableText() {
                return clauseText;
            }

            @Override
            public @Nullable Icon getIcon(boolean unused) {
                return null;
            }
        };
    }

    @Override
    public TreeElement @NotNull [] getChildren() {
        if (!(element instanceof PsiFile)) {
            return EMPTY;
        }

        List<CypherStructureViewElement> clauses = new ArrayList<>();
        ASTNode child = element.getNode().getFirstChildNode();
        while (child != null) {
            if (child.getElementType() == CypherTokenTypes.KEYWORD) {
                String kw = child.getText().toUpperCase(Locale.ENGLISH);
                if (CypherTokenTypes.CLAUSE_START_KEYWORDS.contains(kw)) {
                    clauses.add(new CypherStructureViewElement(child.getPsi()));
                }
            }
            child = child.getTreeNext();
        }
        return clauses.toArray(EMPTY);
    }

    /**
     * Builds a short display string for a clause: the keyword in upper case followed by up to
     * {@value #SNIPPET_MAX_CHARS} characters of subsequent non-whitespace tokens (stopping at the
     * next clause-starting keyword or a semicolon).
     */
    private static String buildClauseSnippet(@NotNull PsiElement kwElement) {
        StringBuilder sb = new StringBuilder(kwElement.getText().toUpperCase(Locale.ENGLISH));
        ASTNode current = kwElement.getNode().getTreeNext();
        while (current != null) {
            IElementType type = current.getElementType();
            if (type != TokenType.WHITE_SPACE) {
                if (type == CypherTokenTypes.SEMICOLON) {
                    break;
                }
                if (type == CypherTokenTypes.KEYWORD) {
                    String kw = current.getText().toUpperCase(Locale.ENGLISH);
                    if (CypherTokenTypes.CLAUSE_START_KEYWORDS.contains(kw)) {
                        break;
                    }
                }
                String text = current.getText();
                if (sb.length() + 1 + text.length() > SNIPPET_MAX_CHARS) {
                    sb.append("...");
                    break;
                }
                sb.append(" ").append(text);
            }
            current = current.getTreeNext();
        }
        return sb.toString();
    }
}
