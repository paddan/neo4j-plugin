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

import java.util.List;

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

        CypherBlock parent = new CypherBlock(root, null, null, CypherIndents.none(), null, 4, false,
                CypherBlock.GroupLayout.forRoot(root));
        List<Block> children = parent.buildChildren();
        Spacing spacing = parent.getSpacing(null, children.get(0));

        assertNotNull(spacing);
        assertEquals(0, lineFeeds(spacing), "First keyword should not be preceded by a blank line");
    }

    @Test
    void movesWhereClauseToNewLine() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "WHERE")
        );

        Spacing spacing = spacingAt(root, 0, 1);

        assertLineBreak(spacing, "WHERE should start on its own line after MATCH");
    }

    @Test
    void keepsClauseContinuationsInline() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "ORDER"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "BY"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "SKIP"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "LIMIT")
        );

        Spacing returnOrder = spacingAt(root, 0, 1);
        Spacing orderBy = spacingAt(root, 1, 2);
        Spacing bySkip = spacingAt(root, 2, 3);
        Spacing skipLimit = spacingAt(root, 3, 4);

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
                StubAstNode.token(CypherTokenTypes.KEYWORD, "LOAD"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CSV"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "WITH")
        );

        Spacing optionalMatch = spacingAt(root, 0, 1);
        Spacing loadCsv = spacingAt(root, 2, 3);
        Spacing csvWith = spacingAt(root, 3, 4);

        assertSingleSpace(optionalMatch, "OPTIONAL MATCH should be kept inline");
        assertSingleSpace(loadCsv, "LOAD CSV should be kept inline");
        assertSingleSpace(csvWith, "CSV WITH should be kept inline");
    }

    @Test
    void indentsMergeActionsAndKeepsBodiesInline() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MERGE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "ON"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CREATE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "SET"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "ON"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "MATCH"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "SET")
        );

        Spacing mergeOn = spacingAt(root, 0, 1);
        Spacing onCreate = spacingAt(root, 1, 2);
        Spacing createSet = spacingAt(root, 2, 3);
        Spacing setOnMatch = spacingAt(root, 3, 4);
        Spacing onMatch = spacingAt(root, 4, 5);
        Spacing matchSet = spacingAt(root, 5, 6);

        assertLineBreak(mergeOn, "Merge actions should start on a new indented line");
        assertSingleSpace(onCreate, "ON CREATE should be kept inline");
        assertSingleSpace(createSet, "SET should stay on the same line as its ON action");
        assertLineBreak(setOnMatch, "Second action should start on its own line");
        assertSingleSpace(onMatch, "ON MATCH should keep MATCH inline");
        assertSingleSpace(matchSet, "SET should stay inline after ON MATCH");
    }

    @Test
    void keepsWhereExistsInline() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "WHERE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "EXISTS")
        );

        Spacing whereExists = spacingAt(root, 0, 1);

        assertSingleSpace(whereExists, "EXISTS should stay on the same line as WHERE");
    }

    @Test
    void breaksLongGroupsFromOutsideIn() {
        String longIdentifier = "a".repeat(90);
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, longIdentifier),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")")
        );

        Spacing afterOpen = spacingAt(root, 1, 2);
        Spacing beforeClose = spacingAt(root, 2, 3);

        assertLineBreak(afterOpen, "Outermost group should break when it exceeds the line length");
        assertLineBreak(beforeClose, "Closing parenthesis should align after a line break");
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

        Spacing beforeBraceOpen = spacingAt(root, 4, 5);
        Spacing afterBraceOpen = spacingAt(root, 5, 6);
        Spacing beforeBraceClose = spacingAt(root, 10, 11);
        Spacing keyColon = spacingAt(root, 6, 7);
        Spacing colonValue = spacingAt(root, 7, 8);

        assertSingleSpace(beforeBraceOpen, "Property map should be separated from labels/types");
        assertNoSpace(afterBraceOpen, "No padding inside property map after '{'");
        assertNoSpace(beforeBraceClose, "No padding inside property map before '}'");
        assertNoSpace(keyColon, "No space between key and colon");
        assertSingleSpace(colonValue, "Space required between colon and value");
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

    @Test
    void breaksCaseBranchesOntoNewLines() {
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

        Spacing caseWhen = spacingAt(root, 1, 2);
        Spacing beforeElse = spacingAt(root, 5, 6);
        Spacing beforeEnd = spacingAt(root, 7, 8);
        Spacing whenThen = spacingAt(root, 3, 4);

        assertLineBreak(caseWhen, "WHEN should start on a new line inside CASE");
        assertLineBreak(beforeElse, "ELSE should start on a new line inside CASE");
        assertLineBreak(beforeEnd, "END should start on a new line inside CASE");
        assertSingleSpace(whenThen, "THEN should stay inline after WHEN condition");
    }

    @Test
    void breaksGroupsContainingCaseExpressions() {
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "CASE"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "WHEN"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "x"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "THEN"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "y"),
                StubAstNode.token(CypherTokenTypes.KEYWORD, "END"),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")")
        );

        Spacing afterOpen = spacingAt(root, 0, 1);
        Spacing beforeClose = spacingAt(root, 6, 7);

        assertLineBreak(afterOpen, "Groups containing CASE expressions should break after the opener");
        assertLineBreak(beforeClose, "Groups containing CASE expressions should break before the closer");
    }

    @Test
    void breaksElementsOfLongListLiteralOntoNewLines() {
        String longIdentifier = "a".repeat(90);
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.BRACKET_OPEN, "["),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, longIdentifier),
                StubAstNode.token(CypherTokenTypes.COMMA, ","),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "b"),
                StubAstNode.token(CypherTokenTypes.BRACKET_CLOSE, "]")
        );

        Spacing afterComma = spacingAt(root, 3, 4);

        assertLineBreak(afterComma, "Each list element should start on a new line when the list literal is broken");
    }

    @Test
    void breaksElementsOfLongMapLiteralOntoNewLines() {
        String longKey = "a".repeat(90);
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.BRACE_OPEN, "{"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, longKey),
                StubAstNode.token(CypherTokenTypes.COLON, ":"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "x"),
                StubAstNode.token(CypherTokenTypes.COMMA, ","),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "b"),
                StubAstNode.token(CypherTokenTypes.COLON, ":"),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "y"),
                StubAstNode.token(CypherTokenTypes.BRACE_CLOSE, "}")
        );

        Spacing afterComma = spacingAt(root, 5, 6);

        assertLineBreak(afterComma, "Each map entry should start on a new line when the map literal is broken");
    }

    @Test
    void doesNotBreakCommasInsideNonBrokenNestedGroups() {
        String longIdentifier = "a".repeat(90);
        StubAstNode root = StubAstNode.root(
                StubAstNode.token(CypherTokenTypes.KEYWORD, "RETURN"),
                StubAstNode.token(CypherTokenTypes.BRACKET_OPEN, "["),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, longIdentifier),
                StubAstNode.token(CypherTokenTypes.COMMA, ","),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "f"),
                StubAstNode.token(CypherTokenTypes.PAREN_OPEN, "("),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "x"),
                StubAstNode.token(CypherTokenTypes.COMMA, ","),
                StubAstNode.token(CypherTokenTypes.IDENTIFIER, "y"),
                StubAstNode.token(CypherTokenTypes.PAREN_CLOSE, ")"),
                StubAstNode.token(CypherTokenTypes.BRACKET_CLOSE, "]")
        );

        CypherBlock parent = new CypherBlock(root, null, null, CypherIndents.none(), null, 4, false,
                CypherBlock.GroupLayout.forRoot(root));
        List<Block> children = parent.buildChildren();
        Spacing spacing = parent.getSpacing(children.get(7), children.get(8));

        assertEquals(null, spacing, "Inner commas should not be forced onto new lines by outer literal wrapping rules");
    }

    private Spacing spacingAt(StubAstNode root, int leftIndex, int rightIndex) {
        CypherBlock parent = new CypherBlock(root, null, null, CypherIndents.none(), null, 4, false,
                CypherBlock.GroupLayout.forRoot(root));
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

    private void assertNoSpace(@Nullable Spacing spacing, String message) {
        assertNotNull(spacing, message);
        assertEquals(0, lineFeeds(spacing), message);
        assertEquals(0, minSpaces(spacing), message);
        assertEquals(0, maxSpaces(spacing), message);
    }

    private int lineFeeds(@NotNull Spacing spacing) {
        return intFromString(spacing, "linefeed", "linefeeds", "linefeedCount");
    }

    private int minSpaces(@NotNull Spacing spacing) {
        return intFromString(spacing, "minspaces", "minSpaces");
    }

    private int maxSpaces(@NotNull Spacing spacing) {
        return intFromString(spacing, "maxspaces", "maxSpaces");
    }

    private int intFromString(@NotNull Spacing spacing, @NotNull String... keys) {
        // Spacing has no stable public getters across platform versions.
        // We use its string representation, but accept multiple common key spellings.
        String asString = spacing.toString();
        if (asString != null) {
            for (String key : keys) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("(?i)" + java.util.regex.Pattern.quote(key) + "=(\\d+)")
                        .matcher(asString);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        throw new AssertionError("Unable to extract any of " + java.util.Arrays.toString(keys)
                + " from Spacing.toString(): " + asString);
    }
}
