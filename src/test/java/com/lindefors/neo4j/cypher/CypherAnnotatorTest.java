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
}
