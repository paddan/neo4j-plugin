package com.lindefors.neo4j.cypher;

import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.formatting.Formatter;
import com.intellij.formatting.FormatterImpl;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CypherFormatterIndentTest {
    @BeforeAll
    static void setUpApplication() {
        if (ApplicationManager.getApplication() == null) {
            Disposable disposable = Disposer.newDisposable();
            MockApplication application = new MockApplication(disposable);
            application.registerService(Formatter.class, new FormatterImpl());
            ApplicationManager.setApplication(application, disposable);
        }
    }

    @Test
    void indentsContentInsideBraces() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CALL"),
                StubAstNode.whitespace(" "),
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MERGE"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "SET"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}"),
                StubAstNode.whitespace(" "),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN")
        );

        List<CypherBlock> tokens = buildBlocks(root);

        assertEquals("CALL", tokens.get(0).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(0).getIndent().getType(), "CALL should not be indented");

        assertEquals("{", tokens.get(1).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(1).getIndent().getType(), "Opening brace stays at base indent");

        assertEquals("MERGE", tokens.get(2).getNode().getText());
        assertEquals(Indent.Type.NORMAL, tokens.get(2).getIndent().getType(), "First statement inside braces should be indented");

        assertEquals("SET", tokens.get(3).getNode().getText());
        assertEquals(Indent.Type.NORMAL, tokens.get(3).getIndent().getType(), "Second statement inside braces should be indented");

        assertEquals("}", tokens.get(4).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(4).getIndent().getType(), "Closing brace should return to base indent");

        assertEquals("RETURN", tokens.get(5).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(5).getIndent().getType(), "Statements after braces return to base indent");
    }

    @Test
    void resetsIndentWhenClosingBraceAppears() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "INNER"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "AFTER")
        );

        List<CypherBlock> tokens = buildBlocks(root);

        assertEquals("{", tokens.get(0).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(0).getIndent().getType(), "Opening brace stays at base indent");

        assertEquals("INNER", tokens.get(1).getNode().getText());
        assertEquals(Indent.Type.NORMAL, tokens.get(1).getIndent().getType(), "Content inside braces should be indented");

        assertEquals("}", tokens.get(2).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(2).getIndent().getType(), "Closing brace resets indent");

        assertEquals("AFTER", tokens.get(3).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(3).getIndent().getType(), "Following tokens stay at base indent");
    }

    @Test
    void indentsNestedCodeBlocks() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CALL"),
                StubAstNode.whitespace(" "),
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "WITH"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}")
        );

        List<CypherBlock> tokens = buildBlocks(root);

        assertEquals("CALL", tokens.get(0).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(0).getIndent().getType(), "Outer clause stays at base indent");

        assertEquals("{", tokens.get(1).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(1).getIndent().getType(), "Outer opening brace is not indented");

        assertEquals("MATCH", tokens.get(2).getNode().getText());
        assertEquals(Indent.Type.NORMAL, tokens.get(2).getIndent().getType(), "First level inside outer block uses normal indent");

        assertEquals("{", tokens.get(3).getNode().getText());
        assertEquals(Indent.Type.NORMAL, tokens.get(3).getIndent().getType(), "Nested block opening aligns with first-level indent");

        assertEquals("RETURN", tokens.get(4).getNode().getText());
        assertEquals(Indent.Type.SPACES, tokens.get(4).getIndent().getType(), "Second level statements get an extra indent");

        assertEquals("}", tokens.get(5).getNode().getText());
        assertEquals(Indent.Type.NORMAL, tokens.get(5).getIndent().getType(), "Closing nested block steps back one level");

        assertEquals("WITH", tokens.get(6).getNode().getText());
        assertEquals(Indent.Type.NORMAL, tokens.get(6).getIndent().getType(), "Content after nested block remains at first level");

        assertEquals("}", tokens.get(7).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(7).getIndent().getType(), "Outer closing brace returns to base indent");
    }

    private List<CypherBlock> buildBlocks(StubAstNode root) {
        List<Block> children = new CypherBlock(root, null, null, CypherIndents.none(), null, 4).buildChildren();
        List<CypherBlock> tokens = new ArrayList<>();
        for (Block child : children) {
            CypherBlock block = (CypherBlock) child;
            if (block.getNode().getElementType() == TokenType.WHITE_SPACE) {
                continue;
            }
            tokens.add(block);
        }
        return tokens;
    }

    private static class StubAstNode implements ASTNode {
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
        public IElementType getElementType() {
            return elementType;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public CharSequence getChars() {
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
        public @Nullable ASTNode getTreeParent() {
            return parent;
        }

        @Override
        public @Nullable ASTNode getFirstChildNode() {
            return children.isEmpty() ? null : children.get(0);
        }

        @Override
        public @Nullable ASTNode getLastChildNode() {
            return children.isEmpty() ? null : children.get(children.size() - 1);
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
        public void addChild(@NotNull ASTNode child) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addChild(@NotNull ASTNode child, ASTNode anchorBefore) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addLeaf(@NotNull IElementType leafType, CharSequence leafText, ASTNode anchorBefore) {
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
        public Object clone() {
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
}
