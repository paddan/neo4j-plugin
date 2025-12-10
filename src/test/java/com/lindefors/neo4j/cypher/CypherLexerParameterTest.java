package com.lindefors.neo4j.cypher;

import com.intellij.lexer.Lexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CypherLexerParameterTest {
    @Test
    void lexesSimpleDollarParameter() {
        List<Token> tokens = lex("$name");

        assertEquals(1, tokens.size(), "Single parameter token expected");
        Token param = tokens.get(0);
        assertEquals(CypherTokenTypes.PARAMETER, param.type);
        assertEquals("$name", param.text);
    }

    @Test
    void lexesParenthesizedParameter() {
        List<Token> tokens = lex("$(userName)");

        assertEquals(1, tokens.size(), "Single parameter token expected");
        Token param = tokens.get(0);
        assertEquals(CypherTokenTypes.PARAMETER, param.type);
        assertEquals("$(userName)", param.text);
    }

    @Test
    void lexesLegacyBracedParameter() {
        List<Token> tokens = lex("{paramName}");

        assertEquals(1, tokens.size(), "Single legacy parameter token expected");
        Token param = tokens.get(0);
        assertEquals(CypherTokenTypes.PARAMETER, param.type);
        assertEquals("{paramName}", param.text);
    }

    @Test
    void lexesLegacyBracedParameterWithWhitespace() {
        List<Token> tokens = lex("{  param_name  }");

        assertEquals(1, tokens.size(), "Single legacy parameter token expected");
        Token param = tokens.get(0);
        assertEquals(CypherTokenTypes.PARAMETER, param.type);
        assertEquals("{  param_name  }", param.text);
    }

    @Test
    void parsesParametersInsideQuery() {
        List<Token> tokens = lex("MATCH (n {id: $id, label: $(label)}) RETURN n");

        IElementType[] expectedTypes = {
                CypherTokenTypes.KEYWORD,
                CypherTokenTypes.PAREN_OPEN,
                CypherTokenTypes.IDENTIFIER,
                CypherTokenTypes.BRACE_OPEN,
                CypherTokenTypes.IDENTIFIER,
                CypherTokenTypes.COLON,
                CypherTokenTypes.PARAMETER,
                CypherTokenTypes.COMMA,
                CypherTokenTypes.IDENTIFIER,
                CypherTokenTypes.COLON,
                CypherTokenTypes.PARAMETER,
                CypherTokenTypes.BRACE_CLOSE,
                CypherTokenTypes.PAREN_CLOSE,
                CypherTokenTypes.KEYWORD,
                CypherTokenTypes.IDENTIFIER
        };

        assertEquals(expectedTypes.length, tokens.size(), "Unexpected token count");
        for (int i = 0; i < expectedTypes.length; i++) {
            assertEquals(expectedTypes[i], tokens.get(i).type, "Token " + i + " type");
            assertNotNull(tokens.get(i).text, "Token text should not be null");
        }
    }

    private List<Token> lex(String source) {
        Lexer lexer = new CypherLexer();
        lexer.start(source, 0, source.length(), 0);
        List<Token> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() != TokenType.WHITE_SPACE) {
                tokens.add(new Token(lexer.getTokenType(), source.substring(lexer.getTokenStart(), lexer.getTokenEnd())));
            }
            lexer.advance();
        }
        return tokens;
    }

    private record Token(IElementType type, String text) {
    }
}
