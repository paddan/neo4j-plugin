package com.lindefors.neo4j.cypher;

import com.intellij.lexer.Lexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CypherLexerCheatSheetKeywordTest {

    @Test
    void lexesCheatSheetKeywordsAsKeywords() {
        List<String> keywords = List.of(
                "LET", "CASE", "WHEN", "THEN", "ELSE", "NEXT", "FINISH",
                "SHOW", "GRANT", "DENY", "REVOKE",
                "TRANSACTIONS", "FUNCTIONS", "PROCEDURES", "SETTINGS",
                "INDEXES", "CONSTRAINTS", "DATABASE", "ALIAS",
                "VECTOR", "RANGE", "FIELDTERMINATOR",
                "NFC", "NORMALIZED"
        );

        for (String keyword : keywords) {
            List<LexedToken> tokens = lex(keyword + " value");
            assertFalse(tokens.isEmpty(), keyword + " should produce at least one token");
            assertEquals(CypherTokenTypes.KEYWORD, tokens.get(0).type(),
                    keyword + " should lex as a keyword");
        }
    }

    private List<LexedToken> lex(String source) {
        Lexer lexer = new CypherLexer();
        lexer.start(source, 0, source.length(), 0);
        List<LexedToken> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() != TokenType.WHITE_SPACE) {
                tokens.add(new LexedToken(lexer.getTokenType(),
                        source.substring(lexer.getTokenStart(), lexer.getTokenEnd())));
            }
            lexer.advance();
        }
        return tokens;
    }

    private record LexedToken(IElementType type, String text) {
    }
}
