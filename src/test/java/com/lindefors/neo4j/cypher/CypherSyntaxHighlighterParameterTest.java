package com.lindefors.neo4j.cypher;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CypherSyntaxHighlighterParameterTest {
    private final CypherSyntaxHighlighter highlighter = new CypherSyntaxHighlighter();

    @Test
    void highlightsSimpleDollarParameter() {
        List<TokenHighlights> tokens = collectTokens("$name");

        assertEquals(1, tokens.size(), "Expected single parameter token");
        assertArrayEquals(new TextAttributesKey[]{CypherSyntaxHighlighter.PARAMETER}, tokens.getFirst().highlights());
    }

    @Test
    void highlightsParenthesizedParameter() {
        List<TokenHighlights> tokens = collectTokens("$(userName)");

        assertEquals(1, tokens.size(), "Expected single parameter token");
        assertArrayEquals(new TextAttributesKey[]{CypherSyntaxHighlighter.PARAMETER}, tokens.getFirst().highlights());
    }

    @Test
    void highlightsLegacyBracedParameter() {
        List<TokenHighlights> tokens = collectTokens("{param}");

        assertEquals(1, tokens.size(), "Expected single legacy parameter token");
        assertArrayEquals(new TextAttributesKey[]{CypherSyntaxHighlighter.PARAMETER}, tokens.getFirst().highlights());
    }

    @Test
    void highlightsParametersInsideQuery() {
        List<TokenHighlights> tokens = collectTokens("RETURN $id, $(other)");

        List<TokenHighlights> parameters = tokens.stream()
                .filter(token -> token.type() == CypherTokenTypes.PARAMETER)
                .toList();

        assertEquals(2, parameters.size(), "Two parameters expected in the query");
        parameters.forEach(token ->
                assertArrayEquals(new TextAttributesKey[]{CypherSyntaxHighlighter.PARAMETER}, token.highlights()));
    }

    @Test
    void parameterUsesParameterFallbackColor() {
        assertEquals(DefaultLanguageHighlighterColors.PARAMETER, CypherSyntaxHighlighter.PARAMETER.getFallbackAttributeKey());
    }

    private List<TokenHighlights> collectTokens(String source) {
        Lexer lexer = highlighter.getHighlightingLexer();
        lexer.start(source, 0, source.length(), 0);

        List<TokenHighlights> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            IElementType type = lexer.getTokenType();
            if (type != TokenType.WHITE_SPACE) {
                tokens.add(new TokenHighlights(type, highlighter.getTokenHighlights(type)));
            }
            lexer.advance();
        }
        return tokens;
    }

    private record TokenHighlights(IElementType type, TextAttributesKey[] highlights) {
    }
}
