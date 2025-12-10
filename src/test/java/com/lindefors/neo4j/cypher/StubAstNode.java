package com.lindefors.neo4j.cypher;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight {@link ASTNode} stub for formatter and spacing tests.
 */
final class StubAstNode implements ASTNode {
    private final IElementType elementType;
    private final String text;
    private StubAstNode treeNext;
    private StubAstNode treePrev;
    private StubAstNode parent;
    private final List<StubAstNode> children = new ArrayList<>();
    private final Map<Key<?>, Object> copyableUserData = new HashMap<>();
    private final Map<Key<?>, Object> userData = new HashMap<>();
    private int startOffset;

    private StubAstNode(IElementType elementType, String text) {
        this.elementType = elementType;
        this.text = text;
    }

    static StubAstNode root(StubAstNode... children) {
        StubAstNode root = new StubAstNode(CypherTokenTypes.KEYWORD, "");
        root.children.addAll(Arrays.asList(children));
        int offset = 0;
        for (int i = 0; i < root.children.size(); i++) {
            StubAstNode child = root.children.get(i);
            child.parent = root;
            child.startOffset = offset;
            offset += child.text.length();
            if (i > 0) {
                StubAstNode prev = root.children.get(i - 1);
                prev.treeNext = child;
                child.treePrev = prev;
            }
        }
        return root;
    }

    static StubAstNode token(IElementType type, String text) {
        return new StubAstNode(type, text);
    }

    static StubAstNode whitespace(String text) {
        return new StubAstNode(TokenType.WHITE_SPACE, text);
    }

    @Override
    public @NotNull IElementType getElementType() {
        return elementType;
    }

    @Override
    public @NotNull String getText() {
        return text;
    }

    @Override
    public @NotNull CharSequence getChars() {
        return text;
    }

    @Override
    public boolean textContains(char c) {
        return text.indexOf(c) >= 0;
    }

    @Override
    public int getStartOffset() {
        return startOffset;
    }

    @Override
    public int getTextLength() {
        return text.length();
    }

    @Override
    public @NotNull TextRange getTextRange() {
        return new TextRange(startOffset, startOffset + text.length());
    }

    @Override
    public ASTNode @NotNull [] getChildren(TokenSet filter) {
        if (filter == null) {
            return children.toArray(ASTNode.EMPTY_ARRAY);
        }
        List<ASTNode> filtered = new ArrayList<>();
        for (StubAstNode child : children) {
            if (filter.contains(child.getElementType())) {
                filtered.add(child);
            }
        }
        return filtered.toArray(ASTNode.EMPTY_ARRAY);
    }

    @Override
    public @Nullable ASTNode getTreeParent() {
        return parent;
    }

    @Override
    public @Nullable ASTNode getFirstChildNode() {
        return children.isEmpty() ? null : children.getFirst();
    }

    @Override
    public @Nullable ASTNode getLastChildNode() {
        return children.isEmpty() ? null : children.getLast();
    }

    @Override
    public @Nullable ASTNode getTreeNext() {
        return treeNext;
    }

    @Override
    public @Nullable ASTNode getTreePrev() {
        return treePrev;
    }

    @Override
    public void addChild(@NotNull ASTNode child) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChild(@NotNull ASTNode child, ASTNode anchorBefore) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addLeaf(@NotNull IElementType leafType, CharSequence leafText, @NotNull ASTNode anchorBefore) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeChild(@NotNull ASTNode child) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRange(@NotNull ASTNode firstNodeToRemove, ASTNode firstNodeToNotRemove) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceChild(@NotNull ASTNode oldChild, @NotNull ASTNode newChild) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceAllChildrenToChildrenOf(@NotNull ASTNode anotherParent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addChildren(@NotNull ASTNode firstChild, ASTNode firstChildToMove, ASTNode anchorBefore) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Object clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull ASTNode copyElement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable ASTNode findLeafElementAt(int offset) {
        return null;
    }

    @Override
    public <T> T getCopyableUserData(@NotNull Key<T> key) {
        //noinspection unchecked
        return (T) copyableUserData.get(key);
    }

    @Override
    public <T> void putCopyableUserData(@NotNull Key<T> key, T value) {
        if (value == null) {
            copyableUserData.remove(key);
        } else {
            copyableUserData.put(key, value);
        }
    }

    @Override
    public @Nullable ASTNode findChildByType(@NotNull IElementType type) {
        for (StubAstNode child : children) {
            if (child.getElementType().equals(type)) {
                return child;
            }
        }
        return null;
    }

    @Override
    public @Nullable ASTNode findChildByType(@NotNull IElementType type, @Nullable ASTNode anchor) {
        if (anchor != null && !children.contains(anchor)) {
            return null;
        }
        int startIndex = anchor == null ? 0 : children.indexOf(anchor) + 1;
        for (int i = startIndex; i < children.size(); i++) {
            StubAstNode child = children.get(i);
            if (child.getElementType().equals(type)) {
                return child;
            }
        }
        return null;
    }

    @Override
    public @Nullable ASTNode findChildByType(@NotNull TokenSet typesSet) {
        for (StubAstNode child : children) {
            if (typesSet.contains(child.getElementType())) {
                return child;
            }
        }
        return null;
    }

    @Override
    public @Nullable ASTNode findChildByType(@NotNull TokenSet typesSet, @Nullable ASTNode anchor) {
        if (anchor != null && !children.contains(anchor)) {
            return null;
        }
        int startIndex = anchor == null ? 0 : children.indexOf(anchor) + 1;
        for (int i = startIndex; i < children.size(); i++) {
            StubAstNode child = children.get(i);
            if (typesSet.contains(child.getElementType())) {
                return child;
            }
        }
        return null;
    }

    @Override
    public @Nullable com.intellij.psi.PsiElement getPsi() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends com.intellij.psi.PsiElement> T getPsi(@NotNull Class<T> aClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable <T> T getUserData(@NotNull Key<T> key) {
        //noinspection unchecked
        return (T) userData.get(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        if (value == null) {
            userData.remove(key);
        } else {
            userData.put(key, value);
        }
    }
}
