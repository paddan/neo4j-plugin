package com.lindefors.neo4j.cypher;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CypherAnnotatorTest {

    private final CypherAnnotator annotator = new CypherAnnotator();

    // --- helpers ---

    private List<CypherAnnotator.LeafToken> tokens(Object... pairs) {
        List<CypherAnnotator.LeafToken> result = new ArrayList<>();
        int offset = 0;
        for (int i = 0; i < pairs.length; i += 2) {
            IElementType type = (IElementType) pairs[i];
            String text = (String) pairs[i + 1];
            result.add(new CypherAnnotator.LeafToken(type, text, new TextRange(offset, offset + text.length())));
            offset += text.length();
        }
        return result;
    }

    // --- semantic highlighting ---

    @Test
    void identifierAfterColonOutsideAnythingIsLabel() {
        // :Person
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "Person"
        ));
        assertEquals(1, anns.size());
        assertEquals(CypherSyntaxHighlighter.LABEL, anns.get(0).attributes());
    }

    @Test
    void identifierAfterColonInsideBracketsIsRelationshipType() {
        // [:KNOWS]
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACKET_OPEN, "[",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "KNOWS",
                CypherTokenTypes.BRACKET_CLOSE, "]"
        ));
        assertEquals(1, anns.size());
        assertEquals(CypherSyntaxHighlighter.RELATIONSHIP_TYPE, anns.get(0).attributes());
    }

    @Test
    void identifierBeforeColonInsideBracesIsPropertyKey() {
        // {name: 'Alice'}
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACE_OPEN, "{",
                CypherTokenTypes.IDENTIFIER, "name",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.STRING, "'Alice'",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));
        assertEquals(1, anns.size());
        assertEquals(CypherSyntaxHighlighter.PROPERTY_KEY, anns.get(0).attributes());
    }

    @Test
    void identifierFollowedByParenIsFunctionName() {
        // count(n)
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.IDENTIFIER, "count",
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.IDENTIFIER, "n",
                CypherTokenTypes.PAREN_CLOSE, ")"
        ));
        assertEquals(1, anns.size());
        assertEquals(CypherSyntaxHighlighter.FUNCTION_NAME, anns.get(0).attributes());
    }

    @Test
    void bareIdentifierGetsNoHighlight() {
        // n (plain variable, no context clues)
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.IDENTIFIER, "n"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void nestedPropertyKeysAreHighlighted() {
        // {a: {b: 1}} — both 'a' and 'b' are property keys
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACE_OPEN, "{",
                CypherTokenTypes.IDENTIFIER, "a",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.BRACE_OPEN, "{",
                CypherTokenTypes.IDENTIFIER, "b",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.NUMBER, "1",
                CypherTokenTypes.BRACE_CLOSE, "}",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));
        assertEquals(2, anns.size());
        assertEquals(CypherSyntaxHighlighter.PROPERTY_KEY, anns.get(0).attributes());
        assertEquals(CypherSyntaxHighlighter.PROPERTY_KEY, anns.get(1).attributes());
    }

    // --- error marking ---

    @Test
    void unmatchedOpenParenIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PAREN_OPEN, "("
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.ERROR, anns.get(0).severity());
        assertEquals("Unmatched '('", anns.get(0).message());
    }

    @Test
    void unmatchedCloseParenIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PAREN_CLOSE, ")"
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.ERROR, anns.get(0).severity());
        assertEquals("Unmatched ')'", anns.get(0).message());
    }

    @Test
    void matchedParensProduceNoErrors() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.IDENTIFIER, "n",
                CypherTokenTypes.PAREN_CLOSE, ")"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void unmatchedOpenBracketIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACKET_OPEN, "["
        ));
        assertEquals(1, anns.size());
        assertEquals("Unmatched '['", anns.get(0).message());
    }

    @Test
    void unmatchedOpenBraceIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACE_OPEN, "{"
        ));
        assertEquals(1, anns.size());
        assertEquals("Unmatched '{'", anns.get(0).message());
    }

    @Test
    void crossedDelimitersProduceErrors() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.BRACKET_OPEN, "[",
                CypherTokenTypes.PAREN_CLOSE, ")",
                CypherTokenTypes.BRACKET_CLOSE, "]"
        ));

        var errors = anns.stream().filter(a -> a.severity() == HighlightSeverity.ERROR).toList();
        assertFalse(errors.isEmpty(), "Crossed delimiters must not be accepted as balanced");
    }

    @Test
    void consecutiveClauseKeywordsProducesWarning() {
        // MATCH RETURN — no content between clause starters
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "MATCH",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "RETURN"
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.WARNING, anns.get(0).severity());
        assertTrue(anns.get(0).message().contains("missing clause body"));
    }

    @Test
    void optionalMatchDoesNotWarn() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "OPTIONAL",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "MATCH"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void detachDeleteDoesNotWarn() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "DETACH",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "DELETE"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void mergeActionSetDoesNotWarn() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "ON",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "CREATE",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "SET",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.IDENTIFIER, "person",
                CypherTokenTypes.DOT, ".",
                CypherTokenTypes.IDENTIFIER, "createdAt",
                CypherTokenTypes.OPERATOR, "=",
                CypherTokenTypes.IDENTIFIER, "datetime",
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.PAREN_CLOSE, ")",
                TokenType.WHITE_SPACE, "\n",
                CypherTokenTypes.KEYWORD, "ON",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "MATCH",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "SET"
        ));

        assertTrue(anns.stream().noneMatch(a -> a.severity() == HighlightSeverity.WARNING));
    }

    @Test
    void matchSetWithoutOnStillWarns() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "MATCH",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "SET"
        ));

        assertTrue(anns.stream().anyMatch(a -> a.severity() == HighlightSeverity.WARNING));
    }

    @Test
    void relationshipTypeInFullMatchPatternIsHighlighted() {
        // MATCH (a)-[:ACTED_IN]->(b)
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "MATCH",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.IDENTIFIER, "a",
                CypherTokenTypes.PAREN_CLOSE, ")",
                CypherTokenTypes.OPERATOR, "-",
                CypherTokenTypes.BRACKET_OPEN, "[",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "ACTED_IN",
                CypherTokenTypes.BRACKET_CLOSE, "]",
                CypherTokenTypes.OPERATOR, "->",
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.IDENTIFIER, "b",
                CypherTokenTypes.PAREN_CLOSE, ")"
        ));
        var highlights = anns.stream().filter(a -> a.attributes() != null).toList();
        assertEquals(1, highlights.size());
        assertEquals(CypherSyntaxHighlighter.RELATIONSHIP_TYPE, highlights.get(0).attributes());
    }

    @Test
    void pipeSeparatedLabelsAllHighlightedAsLabel() {
        // (n:Movie|Actor|Director) — all three labels should be LABEL
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.IDENTIFIER, "n",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "Movie",
                CypherTokenTypes.OPERATOR, "|",
                CypherTokenTypes.IDENTIFIER, "Actor",
                CypherTokenTypes.OPERATOR, "|",
                CypherTokenTypes.IDENTIFIER, "Director",
                CypherTokenTypes.PAREN_CLOSE, ")"
        ));
        var highlights = anns.stream().filter(a -> a.attributes() != null).toList();
        assertEquals(3, highlights.size());
        assertTrue(highlights.stream().allMatch(a -> a.attributes() == CypherSyntaxHighlighter.LABEL));
    }

    @Test
    void pipeSeparatedRelationshipTypesAllHighlightedAsRelType() {
        // [:KNOWS|LIKES] — both should be RELATIONSHIP_TYPE
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACKET_OPEN, "[",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "KNOWS",
                CypherTokenTypes.OPERATOR, "|",
                CypherTokenTypes.IDENTIFIER, "LIKES",
                CypherTokenTypes.BRACKET_CLOSE, "]"
        ));
        var highlights = anns.stream().filter(a -> a.attributes() != null).toList();
        assertEquals(2, highlights.size());
        assertTrue(highlights.stream().allMatch(a -> a.attributes() == CypherSyntaxHighlighter.RELATIONSHIP_TYPE));
    }

    @Test
    void labelsInsideSubqueryBlockAreHighlighted() {
        // CALL { :Person } — Person should still be LABEL inside a subquery brace
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "CALL",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.BRACE_OPEN, "{",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "Person",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));
        var highlights = anns.stream().filter(a -> a.attributes() != null).toList();
        assertEquals(1, highlights.size());
        assertEquals(CypherSyntaxHighlighter.LABEL, highlights.get(0).attributes());
    }

    @Test
    void labelsInsideExistsSubqueryBlockAreHighlighted() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "EXISTS",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.BRACE_OPEN, "{",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "Person",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));
        var highlights = anns.stream().filter(a -> a.attributes() != null).toList();
        assertEquals(1, highlights.size());
        assertEquals(CypherSyntaxHighlighter.LABEL, highlights.get(0).attributes());
    }

    @Test
    void labelsInsideCollectSubqueryBlockAreHighlighted() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "COLLECT",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.BRACE_OPEN, "{",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "Person",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));
        var highlights = anns.stream().filter(a -> a.attributes() != null).toList();
        assertEquals(1, highlights.size());
        assertEquals(CypherSyntaxHighlighter.LABEL, highlights.get(0).attributes());
    }

    @Test
    void labelsInsideCountSubqueryBlockAreHighlighted() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "COUNT",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.BRACE_OPEN, "{",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "Person",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));

        var highlights = anns.stream().filter(a -> a.attributes() != null).toList();
        assertEquals(1, highlights.size());
        assertEquals(CypherSyntaxHighlighter.LABEL, highlights.get(0).attributes());
    }

    @Test
    void labelsInsideScopedCallSubqueryBlockAreHighlighted() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "CALL",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.IDENTIFIER, "p",
                CypherTokenTypes.PAREN_CLOSE, ")",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.BRACE_OPEN, "{",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "Person",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));

        var highlights = anns.stream().filter(a -> a.attributes() != null).toList();
        assertEquals(1, highlights.size());
        assertEquals(CypherSyntaxHighlighter.LABEL, highlights.get(0).attributes());
    }

    @Test
    void mapValueAfterColonIsNotHighlightedAsLabel() {
        // { actor: node, director: director_name } — values must NOT be LABEL
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACE_OPEN, "{",
                CypherTokenTypes.IDENTIFIER, "actor",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "node",
                CypherTokenTypes.COMMA, ",",
                CypherTokenTypes.IDENTIFIER, "director",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "director_name",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));
        var labels = anns.stream()
                .filter(a -> a.attributes() == CypherSyntaxHighlighter.LABEL)
                .toList();
        assertTrue(labels.isEmpty(), "map values should not be highlighted as labels");
        var propKeys = anns.stream()
                .filter(a -> a.attributes() == CypherSyntaxHighlighter.PROPERTY_KEY)
                .toList();
        assertEquals(2, propKeys.size(), "actor and director should be property keys");
    }

    // --- unterminated literals ---

    @Test
    void unterminatedSingleQuotedStringIsError() {
        // 'abc — no closing quote
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.STRING, "'abc"
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.ERROR, anns.get(0).severity());
        assertTrue(anns.get(0).message().toLowerCase().contains("unterminated"));
    }

    @Test
    void unterminatedDoubleQuotedStringIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.STRING, "\"abc"
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.ERROR, anns.get(0).severity());
    }

    @Test
    void terminatedStringProducesNoError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.STRING, "'abc'"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void unterminatedBacktickIdentifierIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.IDENTIFIER, "`abc"
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.ERROR, anns.get(0).severity());
        assertTrue(anns.get(0).message().toLowerCase().contains("unterminated"));
    }

    @Test
    void terminatedBacktickIdentifierProducesNoError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.IDENTIFIER, "`abc`"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void unterminatedBlockCommentIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.COMMENT, "/* abc"
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.ERROR, anns.get(0).severity());
        assertTrue(anns.get(0).message().toLowerCase().contains("unterminated"));
    }

    @Test
    void terminatedBlockCommentProducesNoError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.COMMENT, "/* abc */"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void unterminatedParenthesizedParameterIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PARAMETER, "$(foo"
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.ERROR, anns.get(0).severity());
        assertTrue(anns.get(0).message().toLowerCase().contains("unterminated"));
    }

    @Test
    void terminatedParenthesizedParameterProducesNoError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PARAMETER, "$(foo)"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void simpleDollarParameterProducesNoError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PARAMETER, "$name"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void lineCommentProducesNoError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.COMMENT, "// trailing"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void clauseWithContentThenNewClauseDoesNotWarn() {
        // MATCH (n) RETURN n — content between MATCH and RETURN
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "MATCH",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.IDENTIFIER, "n",
                CypherTokenTypes.PAREN_CLOSE, ")",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "RETURN",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.IDENTIFIER, "n"
        ));
        assertTrue(anns.isEmpty());
    }
}
