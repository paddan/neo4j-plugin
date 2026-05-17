package com.lindefors.neo4j.cypher;

import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CypherFoldingBuilderTest {

    private final CypherFoldingBuilder foldingBuilder = new CypherFoldingBuilder();

    @Test
    void foldsMultilineBlockComment() {
        Document doc = new DocumentImpl("/* this is a\nblock comment */\nMATCH");
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.COMMENT, "/* this is a\nblock comment */"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH")
        );

        FoldingDescriptor[] descriptors = foldingBuilder.buildFoldRegions(root, doc);

        assertEquals(1, descriptors.length, "Multiline block comment should produce a fold region");
        assertEquals("/* ... */", foldingBuilder.getPlaceholderText(descriptors[0].getElement()));
    }

    @Test
    void doesNotFoldSingleLineBlockComment() {
        Document doc = new DocumentImpl("/* inline */ MATCH");
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.COMMENT, "/* inline */"),
                StubAstNode.whitespace(" "),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH")
        );

        FoldingDescriptor[] descriptors = foldingBuilder.buildFoldRegions(root, doc);

        assertEquals(0, descriptors.length, "Single-line block comment should not produce a fold region");
    }

    @Test
    void doesNotFoldLineComment() {
        Document doc = new DocumentImpl("// line comment\nMATCH");
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.COMMENT, "// line comment"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH")
        );

        FoldingDescriptor[] descriptors = foldingBuilder.buildFoldRegions(root, doc);

        assertEquals(0, descriptors.length, "Line comments should not produce fold regions");
    }

    @Test
    void foldsMultilineParentheses() {
        Document doc = new DocumentImpl("(\n  a,\n  b\n)");
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.whitespace("\n  "),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "a"),
                StubAstNode.token(CypherTokenTypes.COMMA, ","),
                StubAstNode.whitespace("\n  "),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "b"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")")
        );

        FoldingDescriptor[] descriptors = foldingBuilder.buildFoldRegions(root, doc);

        assertEquals(1, descriptors.length, "Multiline parentheses should produce a fold region");
        assertEquals("(...)", foldingBuilder.getPlaceholderText(descriptors[0].getElement()));
    }

    @Test
    void foldsMultilineBrackets() {
        Document doc = new DocumentImpl("[\n  a,\n  b\n]");
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.BRACKET_OPEN, "["),
                StubAstNode.whitespace("\n  "),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "a"),
                StubAstNode.token(CypherTokenTypes.COMMA, ","),
                StubAstNode.whitespace("\n  "),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "b"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.BRACKET_CLOSE, "]")
        );

        FoldingDescriptor[] descriptors = foldingBuilder.buildFoldRegions(root, doc);

        assertEquals(1, descriptors.length, "Multiline brackets should produce a fold region");
        assertEquals("[...]", foldingBuilder.getPlaceholderText(descriptors[0].getElement()));
    }

    @Test
    void foldsMultilineBraces() {
        Document doc = new DocumentImpl("{\n  key: value\n}");
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.whitespace("\n  "),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "key"),
                StubAstNode.token(CypherTokenTypes.COLON, ":"),
                StubAstNode.whitespace(" "),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "value"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}")
        );

        FoldingDescriptor[] descriptors = foldingBuilder.buildFoldRegions(root, doc);

        assertEquals(1, descriptors.length, "Multiline braces should produce a fold region");
        assertEquals("{...}", foldingBuilder.getPlaceholderText(descriptors[0].getElement()));
    }

    @Test
    void doesNotFoldSingleLineDelimiters() {
        Document doc = new DocumentImpl("(a) [b] {c}");
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "a"),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")"),
                StubAstNode.whitespace(" "),
                StubAstNode.token(CypherTokenTypes.BRACKET_OPEN, "["),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "b"),
                StubAstNode.token(CypherTokenTypes.BRACKET_CLOSE, "]"),
                StubAstNode.whitespace(" "),
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "c"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}")
        );

        FoldingDescriptor[] descriptors = foldingBuilder.buildFoldRegions(root, doc);

        assertEquals(0, descriptors.length, "Single-line delimiters should not produce fold regions");
    }

    @Test
    void handlesNestedMultilineDelimiters() {
        Document doc = new DocumentImpl("(\n  [\n    a\n  ]\n)");
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.whitespace("\n  "),
                StubAstNode.token(CypherTokenTypes.BRACKET_OPEN, "["),
                StubAstNode.whitespace("\n    "),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "a"),
                StubAstNode.whitespace("\n  "),
                StubAstNode.token(CypherTokenTypes.BRACKET_CLOSE, "]"),
                StubAstNode.whitespace("\n"),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")")
        );

        FoldingDescriptor[] descriptors = foldingBuilder.buildFoldRegions(root, doc);

        assertEquals(2, descriptors.length, "Nested multiline delimiters should both produce fold regions");
    }

    @Test
    void isCollapsedByDefaultReturnsFalse() {
        StubAstNode node = StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH");
        assertFalse(foldingBuilder.isCollapsedByDefault(node));
    }

    @Test
    void unknownNodeTypePlaceholderReturnsEllipsis() {
        StubAstNode node = StubAstNode.token(CypherTokenTypes.IDENTIFIER, "x");
        assertEquals("...", foldingBuilder.getPlaceholderText(node));
    }
}
