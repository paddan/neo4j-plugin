package com.lindefors.neo4j.cypher;

import com.intellij.formatting.Block;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Formatter;
import com.intellij.formatting.FormatterImpl;
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.TokenType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

        assertEquals("CALL", tokens.getFirst().getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.getFirst().getIndent().getType(), "CALL should not be indented");

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

        assertEquals("{", tokens.getFirst().getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.getFirst().getIndent().getType(), "Opening brace stays at base indent");

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

        assertEquals("CALL", tokens.getFirst().getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.getFirst().getIndent().getType(), "Outer clause stays at base indent");

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
}
