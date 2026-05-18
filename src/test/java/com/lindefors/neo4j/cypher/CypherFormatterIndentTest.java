package com.lindefors.neo4j.cypher;

import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
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

        assertEquals("CALL", tokens.get(0).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(0).getIndent().getType(), "CALL should not be indented");

        assertEquals("{", tokens.get(1).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(1).getIndent().getType(), "Opening brace stays at base indent");

        assertEquals("MERGE", tokens.get(2).getNode().getText());
        assertEquals(Indent.Type.SPACES, tokens.get(2).getIndent().getType(), "First statement inside braces should be indented");

        assertEquals("SET", tokens.get(3).getNode().getText());
        assertEquals(Indent.Type.SPACES, tokens.get(3).getIndent().getType(), "Second statement inside braces should be indented");

        assertEquals("}", tokens.get(4).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(4).getIndent().getType(), "Closing brace should return to base indent");

        assertEquals("RETURN", tokens.get(5).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(5).getIndent().getType(), "Statements after braces return to base indent");
    }

    @Test
    void resetsIndentWhenClosingBraceAppears() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "AFTER")
        );

        List<CypherBlock> tokens = buildBlocks(root);

        assertEquals("{", tokens.get(0).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(0).getIndent().getType(), "Opening brace stays at base indent");

        assertEquals("MATCH", tokens.get(1).getNode().getText());
        assertEquals(Indent.Type.SPACES, tokens.get(1).getIndent().getType(), "Content inside braces should be indented");

        assertEquals("}", tokens.get(2).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(2).getIndent().getType(), "Closing brace resets indent");

        assertEquals("AFTER", tokens.get(3).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(3).getIndent().getType(), "Following tokens stay at base indent");
    }

    @Test
    void indentsBrokenMapElementsByTwoSpaces() {
        String longKey = "a".repeat(90);
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, longKey),
                StubAstNode.token(CypherTokenTypes.COLON, ":"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "value"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}")
        );

        List<CypherBlock> tokens = buildBlocks(root);

        assertEquals("{", tokens.get(1).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(1).getIndent().getType(), "Opening brace stays at base indent");

        assertEquals(longKey, tokens.get(2).getNode().getText());
        assertEquals(Indent.getSpaceIndent(2).toString(), tokens.get(2).getIndent().toString(),
                "Broken map elements should be indented by two spaces");

        assertEquals("}", tokens.get(5).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(5).getIndent().getType(), "Closing brace returns to base indent");
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
        assertEquals(Indent.Type.SPACES, tokens.get(2).getIndent().getType(), "First level inside outer block uses normal indent");

        assertEquals("{", tokens.get(3).getNode().getText());
        assertEquals(Indent.Type.SPACES, tokens.get(3).getIndent().getType(), "Nested block opening aligns with first-level indent");

        assertEquals("RETURN", tokens.get(4).getNode().getText());
        assertEquals(Indent.Type.SPACES, tokens.get(4).getIndent().getType(), "Second level statements get an extra indent");

        assertEquals("}", tokens.get(5).getNode().getText());
        assertEquals(Indent.Type.SPACES, tokens.get(5).getIndent().getType(), "Closing nested block steps back one level");

        assertEquals("WITH", tokens.get(6).getNode().getText());
        assertEquals(Indent.Type.SPACES, tokens.get(6).getIndent().getType(), "Content after nested block remains at first level");

        assertEquals("}", tokens.get(7).getNode().getText());
        assertEquals(Indent.Type.NONE, tokens.get(7).getIndent().getType(), "Outer closing brace returns to base indent");
    }

    @Test
    void indentsMergeActionsWithTwoSpaces() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MERGE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "ON"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CREATE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "SET"),
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MERGE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "ON"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "SET"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}")
        );

        List<CypherBlock> tokens = buildBlocks(root);
        Indent twoSpaces = Indent.getSpaceIndent(2);
        Indent sixSpaces = Indent.getSpaceIndent(6);

        assertEquals(twoSpaces.toString(), tokens.get(1).getIndent().toString(),
                "ON CREATE should indent two spaces relative to MERGE");
        assertEquals(sixSpaces.toString(), tokens.get(6).getIndent().toString(),
                "ON MATCH inside braces should add two spaces on top of the brace indent");
    }

    @Test
    void addsTwoSpacesPerBrokenGroup() {
        String longIdentifier = "z".repeat(90);
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "outer"),
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, longIdentifier),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")"),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")")
        );

        List<CypherBlock> tokens = buildBlocks(root);

        assertEquals(Indent.getSpaceIndent(2).toString(), tokens.get(2).getIndent().toString(),
                "Content inside a broken group should be indented by two spaces");
        assertEquals(Indent.getSpaceIndent(4).toString(), tokens.get(4).getIndent().toString(),
                "Nested broken group content should add two more spaces");
    }

    @Test
    void usesContinuationIndentForTabsInNestedBlocks() {
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

        List<CypherBlock> tokens = buildBlocks(root, true);

        assertEquals(Indent.Type.NONE, tokens.get(0).getIndent().getType(), "Outer clause stays at base indent");
        assertEquals(Indent.Type.NORMAL, tokens.get(2).getIndent().getType(), "First level inside outer block uses normal indent");
        assertEquals(Indent.Type.CONTINUATION_WITHOUT_FIRST, tokens.get(4).getIndent().getType(),
                "Nested content should use continuation indent when tabs are configured");
    }

    @Test
    void indentsWhenAndElseByTwoSpacesInsideCaseExpression() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CASE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "WHEN"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "x"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "THEN"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "y"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "ELSE"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "z"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "END")
        );

        List<CypherBlock> tokens = buildBlocks(root);

        assertEquals(Indent.getSpaceIndent(2).toString(), tokens.get(2).getIndent().toString(),
                "WHEN should be indented by two spaces relative to CASE");
        assertEquals(Indent.getSpaceIndent(2).toString(), tokens.get(6).getIndent().toString(),
                "ELSE should be indented by two spaces relative to CASE");
        assertEquals(Indent.Type.NONE, tokens.get(8).getIndent().getType(),
                "END should return to the CASE indentation level");
    }

    @Test
    void keepsBaseIndentBetweenTopLevelKeywords() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN")
        );
        CypherBlock block = new CypherBlock(
                root,
                null,
                null,
                CypherIndents.none(),
                null,
                4,
                false,
                CypherBlock.GroupLayout.forRoot(root, 80)
        );

        ChildAttributes attributes = block.getChildAttributes(1);

        assertEquals(Indent.Type.NONE, attributes.getChildIndent().getType(),
                "New lines between top-level keywords should not add indentation");
    }

    @Test
    void indentsNewLinesInsideBraces() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}")
        );
        CypherBlock block = new CypherBlock(
                root,
                null,
                null,
                CypherIndents.none(),
                null,
                4,
                false,
                CypherBlock.GroupLayout.forRoot(root, 80)
        );

        ChildAttributes afterOpeningBrace = block.getChildAttributes(1);
        ChildAttributes afterIndentedContent = block.getChildAttributes(2);
        ChildAttributes afterClosingBrace = block.getChildAttributes(3);

        assertEquals(Indent.Type.SPACES, afterOpeningBrace.getChildIndent().getType(),
                "Content directly inside braces should be indented");
        assertEquals(Indent.Type.SPACES, afterIndentedContent.getChildIndent().getType(),
                "Indentation stays while inside brace scope");
        assertEquals(Indent.Type.NONE, afterClosingBrace.getChildIndent().getType(),
                "Indentation resets after closing brace");
    }

    private List<CypherBlock> buildBlocks(StubAstNode root) {
        return buildBlocks(root, false);
    }

    private List<CypherBlock> buildBlocks(StubAstNode root, boolean useTabs) {
        List<Block> children = new CypherBlock(root, null, null, CypherIndents.none(), null, 4, useTabs,
                CypherBlock.GroupLayout.forRoot(root, 80)).buildChildren();
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
