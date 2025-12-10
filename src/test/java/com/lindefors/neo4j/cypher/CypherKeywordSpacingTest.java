package com.lindefors.neo4j.cypher;

import com.intellij.formatting.Block;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Formatter;
import com.intellij.formatting.FormatterImpl;
import com.intellij.mock.MockApplication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CypherKeywordSpacingTest {
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
    void insertsLineBreaksBetweenTopLevelClauses() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN")
        );

        Spacing spacing = spacingAt(root, 0, 1);

        assertNotNull(spacing);
        assertEquals(1, lineFeeds(spacing), "RETURN should start on a new line");
    }

    @Test
    void doesNotAddLeadingLineBreakBeforeFirstKeyword() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN")
        );

        CypherBlock parent = new CypherBlock(root, null, null, CypherIndents.none(), null, 4);
        List<Block> children = parent.buildChildren();
        Spacing spacing = parent.getSpacing(null, children.get(0));

        assertNotNull(spacing);
        assertEquals(0, lineFeeds(spacing), "First keyword should not be preceded by a blank line");
    }

    @Test
    void keepsClauseContinuationsInline() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "WHERE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "ORDER"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "BY"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "SKIP"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "LIMIT")
        );

        Spacing matchWhere = spacingAt(root, 0, 1);
        Spacing returnOrder = spacingAt(root, 2, 3);
        Spacing orderBy = spacingAt(root, 3, 4);
        Spacing bySkip = spacingAt(root, 4, 5);
        Spacing skipLimit = spacingAt(root, 5, 6);

        assertSingleSpace(matchWhere, "WHERE should stay on the same line as MATCH");
        assertSingleSpace(returnOrder, "ORDER should stay on the same line as RETURN");
        assertSingleSpace(orderBy, "ORDER BY should stay on the same line");
        assertSingleSpace(bySkip, "SKIP should stay on the same line");
        assertSingleSpace(skipLimit, "LIMIT should stay on the same line");
    }

    @Test
    void keepsCompoundKeywordsTogether() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "OPTIONAL"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MERGE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "ON"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CREATE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "LOAD"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CSV"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "WITH")
        );

        Spacing optionalMatch = spacingAt(root, 0, 1);
        Spacing mergeOn = spacingAt(root, 2, 3);
        Spacing onCreate = spacingAt(root, 3, 4);
        Spacing loadCsv = spacingAt(root, 5, 6);
        Spacing csvWith = spacingAt(root, 6, 7);

        assertSingleSpace(optionalMatch, "OPTIONAL MATCH should be kept inline");
        assertSingleSpace(mergeOn, "ON should continue the MERGE clause");
        assertSingleSpace(onCreate, "ON CREATE should be kept inline");
        assertSingleSpace(loadCsv, "LOAD CSV should be kept inline");
        assertSingleSpace(csvWith, "CSV WITH should be kept inline");
    }

    @Test
    void keepsInlineMapsInsidePatterns() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MERGE"),
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "m"),
                StubAstNode.token(CypherTokenTypes.COLON, ":"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "Movie"),
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "movieId"),
                StubAstNode.token(CypherTokenTypes.COLON, ":"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "row"),
                StubAstNode.token(CypherTokenTypes.DOT, "."),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "movieId"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}"),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")")
        );

        Spacing afterBraceOpen = spacingAt(root, 5, 6);
        Spacing beforeBraceClose = spacingAt(root, 10, 11);

        assertSingleSpace(afterBraceOpen, "Properties inside braces should stay inline");
        assertSingleSpace(beforeBraceClose, "Closing brace should stay inline with properties");
    }

    @Test
    void keepsRelationshipOperatorsTight() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "n"),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")"),
                StubAstNode.token(CypherTokenTypes.OPERATOR, "-->"),
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "m"),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")")
        );

        Spacing beforeArrow = spacingAt(root, 2, 3);
        Spacing afterArrow = spacingAt(root, 3, 4);

        assertNotNull(beforeArrow);
        assertEquals(0, minSpaces(beforeArrow), "No spaces should be added before relationship arrows");
        assertEquals(0, maxSpaces(beforeArrow), "No spaces should be added before relationship arrows");

        assertNotNull(afterArrow);
        assertEquals(0, minSpaces(afterArrow), "No spaces should be added after relationship arrows");
        assertEquals(0, maxSpaces(afterArrow), "No spaces should be added after relationship arrows");
    }

    @Test
    void movesClosingBraceOfCodeBlockToNewLine() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CALL"),
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN")
        );

        Spacing afterBraceOpen = spacingAt(root, 1, 2);
        Spacing beforeBraceClose = spacingAt(root, 3, 4);

        assertLineBreak(afterBraceOpen, "Subquery content should start on a new line");
        assertLineBreak(beforeBraceClose, "Closing brace should be on its own line when the block is indented");
    }

    private Spacing spacingAt(StubAstNode root, int leftIndex, int rightIndex) {
        CypherBlock parent = new CypherBlock(root, null, null, CypherIndents.none(), null, 4);
        List<Block> children = parent.buildChildren();
        return parent.getSpacing(children.get(leftIndex), children.get(rightIndex));
    }

    private void assertSingleSpace(@Nullable Spacing spacing, String message) {
        assertNotNull(spacing, message);
        assertEquals(0, lineFeeds(spacing), message);
        assertEquals(1, minSpaces(spacing), message);
        assertEquals(1, maxSpaces(spacing), message);
    }

    private void assertLineBreak(@Nullable Spacing spacing, String message) {
        assertNotNull(spacing, message);
        assertEquals(1, lineFeeds(spacing), message);
    }

    private int lineFeeds(@NotNull Spacing spacing) {
        return intField(spacing, "linefeed");
    }

    private int minSpaces(@NotNull Spacing spacing) {
        return intField(spacing, "minspaces");
    }

    private int maxSpaces(@NotNull Spacing spacing) {
        return intField(spacing, "maxspaces");
    }

    private int intField(@NotNull Spacing spacing, @NotNull String identifierPart) {
        for (Field field : spacing.getClass().getDeclaredFields()) {
            if (field.getName().toLowerCase(Locale.ENGLISH).contains(identifierPart)) {
                try {
                    field.setAccessible(true);
                    return field.getInt(spacing);
                } catch (IllegalAccessException e) {
                    throw new AssertionError("Unable to read spacing field: " + field.getName(), e);
                }
            }
        }
        throw new AssertionError("Spacing is missing expected field containing '" + identifierPart + "'");
    }
}
