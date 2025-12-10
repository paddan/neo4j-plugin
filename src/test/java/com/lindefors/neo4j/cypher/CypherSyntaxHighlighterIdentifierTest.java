package com.lindefors.neo4j.cypher;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherSyntaxHighlighterIdentifierTest {
    private final CypherSyntaxHighlighter highlighter = new CypherSyntaxHighlighter();

    @Test
    void highlightsIdentifiersAsVariables() {
        List<TokenHighlights> tokens = collectTokens("MATCH (u)-[r:KNOWS]->(friend) RETURN u, r, friend");

        List<TokenHighlights> identifiers = tokens.stream()
                .filter(token -> token.type() == CypherTokenTypes.IDENTIFIER)
                .toList();

        identifiers.forEach(identifier ->
                assertArrayEquals(new TextAttributesKey[]{CypherSyntaxHighlighter.IDENTIFIER}, identifier.highlights()));

        Set<String> variableNames = identifiers.stream()
                .map(TokenHighlights::text)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(variableNames.containsAll(Set.of("u", "r", "friend")), "All variables should be highlighted");
    }

    private List<TokenHighlights> collectTokens(String source) {
        Lexer lexer = highlighter.getHighlightingLexer();
        lexer.start(source, 0, source.length(), 0);

        List<TokenHighlights> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            IElementType type = lexer.getTokenType();
            if (type != TokenType.WHITE_SPACE) {
                tokens.add(new TokenHighlights(type,
                        source.substring(lexer.getTokenStart(), lexer.getTokenEnd()),
                        highlighter.getTokenHighlights(type)));
            }
            lexer.advance();
        }
        return tokens;
    }

    private record TokenHighlights(IElementType type, String text, TextAttributesKey[] highlights) {
    }
}
