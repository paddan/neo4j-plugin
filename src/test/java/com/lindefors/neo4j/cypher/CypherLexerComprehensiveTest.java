package com.lindefors.neo4j.cypher;

import com.intellij.lexer.Lexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherLexerComprehensiveTest {

    // --- All declared keywords lex as keywords ---

    @Test
    void allDeclaredKeywordsLexAsKeywords() {
        for (String keyword : CypherTokenTypes.KEYWORDS) {
            List<Token> tokens = lex(keyword + " x");
            assertFalse(tokens.isEmpty(), keyword + " should produce at least one token");
            assertEquals(CypherTokenTypes.KEYWORD, tokens.get(0).type,
                    keyword + " should lex as KEYWORD");
        }
    }

    // --- Keywords are recognized case-insensitively ---

    @Test
    void keywordsAreRecognizedCaseInsensitively() {
        for (String keyword : CypherTokenTypes.KEYWORDS) {
            String lower = keyword.toLowerCase(Locale.ENGLISH);
            List<Token> tokens = lex(lower + " x");
            assertEquals(CypherTokenTypes.KEYWORD, tokens.get(0).type,
                    "Lowercase " + lower + " should lex as KEYWORD");

            String mixed = keyword.substring(0, 1).toLowerCase(Locale.ENGLISH)
                    + keyword.substring(1).toUpperCase(Locale.ENGLISH);
            if (keyword.length() > 1) {
                tokens = lex(mixed + " x");
                assertEquals(CypherTokenTypes.KEYWORD, tokens.get(0).type,
                        "Mixed-case " + mixed + " should lex as KEYWORD");
            }
        }
    }

    // --- All CLAUSE_START_KEYWORDS are in the main KEYWORDS set ---

    @Test
    void allClauseStartKeywordsAreInMainKeywordsSet() {
        for (String clauseStart : CypherTokenTypes.CLAUSE_START_KEYWORDS) {
            assertTrue(CypherTokenTypes.KEYWORDS.contains(clauseStart),
                    clauseStart + " from CLAUSE_START_KEYWORDS must be in KEYWORDS");
        }
    }

    // --- All CLAUSE_CONTINUATION_KEYWORDS are in the main KEYWORDS set ---

    @Test
    void allClauseContinuationKeywordsAreInMainKeywordsSet() {
        for (String continuation : CypherTokenTypes.CLAUSE_CONTINUATION_KEYWORDS) {
            assertTrue(CypherTokenTypes.KEYWORDS.contains(continuation),
                    continuation + " from CLAUSE_CONTINUATION_KEYWORDS must be in KEYWORDS");
        }
    }

    // --- All functions lex as identifiers (not keywords) ---

    @Test
    void functionsLexAsIdentifiers() {
        for (String function : CypherFunctions.FUNCTIONS) {
            String name = function.replace("()", "");
            List<Token> tokens = lex(name + "(x)");
            assertFalse(tokens.isEmpty(), function + " should produce at least one token");
            assertEquals(CypherTokenTypes.IDENTIFIER, tokens.get(0).type,
                    function + " should lex as IDENTIFIER, not KEYWORD");
        }
    }

    // --- Functions don't shadow keywords ---

    @Test
    void functionNamesDoNotConflictWithKeywords() {
        Set<String> keywordSet = CypherTokenTypes.KEYWORDS;
        Set<String> keywordFunctions = CypherTokenTypes.KEYWORD_FUNCTIONS;
        for (String function : CypherFunctions.FUNCTIONS) {
            String name = function.replace("()", "");
            String upper = name.toUpperCase(Locale.ENGLISH);
            if (keywordFunctions.contains(upper)) {
                continue; // dual-purpose: lexer disambiguates by lookahead for '('
            }
            assertFalse(keywordSet.contains(upper),
                    function + " name '" + name + "' must not be a keyword");
        }
    }

    // --- Multiple clause-start keywords in sequence are each recognized ---

    @Test
    void sequenceOfKeywordsLexesAllAsKeywords() {
        List<Token> tokens = lex("MATCH OPTIONAL MATCH WHERE RETURN ORDER BY SKIP LIMIT");
        for (Token token : tokens) {
            assertEquals(CypherTokenTypes.KEYWORD, token.type,
                    token.text + " should lex as KEYWORD in keyword sequence");
        }
        assertEquals(9, tokens.size(), "Expected 9 keyword tokens");
    }

    // --- Backtick-quoted identifiers work ---

    @Test
    void backtickQuotedIdentifiersLexAsIdentifiers() {
        List<Token> tokens = lex("MATCH (`weird name`) RETURN `weird name`");

        assertEquals(CypherTokenTypes.KEYWORD, tokens.get(0).type);
        assertEquals("MATCH", tokens.get(0).text);
        assertEquals(CypherTokenTypes.PAREN_OPEN, tokens.get(1).type);
        assertEquals(CypherTokenTypes.IDENTIFIER, tokens.get(2).type);
        assertEquals("`weird name`", tokens.get(2).text);
        assertEquals(CypherTokenTypes.PAREN_CLOSE, tokens.get(3).type);
        assertEquals(CypherTokenTypes.KEYWORD, tokens.get(4).type);
        assertEquals(CypherTokenTypes.IDENTIFIER, tokens.get(5).type);
        assertEquals("`weird name`", tokens.get(5).text);
    }

    // --- Unterminated backtick identifier spans to EOF ---

    @Test
    void unterminatedBacktickIdentifierLexesToEof() {
        List<Token> tokens = lex("`unterminated");
        assertEquals(1, tokens.size(), "Single token expected");
        assertEquals(CypherTokenTypes.IDENTIFIER, tokens.get(0).type);
        assertEquals("`unterminated", tokens.get(0).text);
    }

    // --- Block comment lexing ---

    @Test
    void blockCommentLexesAsComment() {
        List<Token> tokens = lex("/* block comment */ MATCH");
        assertEquals(2, tokens.size());
        assertEquals(CypherTokenTypes.COMMENT, tokens.get(0).type);
        assertEquals("/* block comment */", tokens.get(0).text);
        assertEquals(CypherTokenTypes.KEYWORD, tokens.get(1).type);
        assertEquals("MATCH", tokens.get(1).text);
    }

    // --- Unterminated block comment spans to EOF ---

    @Test
    void unterminatedBlockCommentLexesToEof() {
        List<Token> tokens = lex("/* unterminated");
        assertEquals(1, tokens.size(), "Single token expected");
        assertEquals(CypherTokenTypes.COMMENT, tokens.get(0).type);
        assertTrue(tokens.get(0).text.startsWith("/*"));
    }

    // --- Line comment lexing ---

    @Test
    void lineCommentLexesAsComment() {
        List<Token> tokens = lex("// line comment\nMATCH");
        assertEquals(2, tokens.size(), "Expected 2 tokens: comment and keyword");
        assertEquals(CypherTokenTypes.COMMENT, tokens.get(0).type);
        assertEquals("// line comment", tokens.get(0).text);
        assertEquals(CypherTokenTypes.KEYWORD, tokens.get(1).type);
        assertEquals("MATCH", tokens.get(1).text);
    }

    // --- Operators lex correctly ---

    @Test
    void operatorsLexCorrectly() {
        List<Token> tokens = lex("= <> < > <= >= + - * / % ^");
        for (Token token : tokens) {
            assertEquals(CypherTokenTypes.OPERATOR, token.type,
                    token.text + " should lex as OPERATOR");
        }
    }

    // --- Multichar operators lex as single tokens ---

    @Test
    void multicharOperatorsLexAsSingleTokens() {
        List<Token> tokens = lex("--> <-- <->");
        assertEquals(3, tokens.size(), "Expected 3 operator tokens");
        for (Token token : tokens) {
            assertEquals(CypherTokenTypes.OPERATOR, token.type);
        }
        assertEquals("-->", tokens.get(0).text);
        assertEquals("<--", tokens.get(1).text);
        assertEquals("<->", tokens.get(2).text);
    }

    // --- Keyword count consistency ---

    @Test
    void keywordCountIsConsistent() {
        int totalKeywords = CypherTokenTypes.KEYWORDS.size();
        assertTrue(totalKeywords > 50, "Should have more than 50 keywords, got " + totalKeywords);
    }

    // --- CLAUSE_START and CLAUSE_CONTINUATION are disjoint ---

    @Test
    void clauseStartAndContinuationAreDisjoint() {
        Set<String> intersection = CypherTokenTypes.CLAUSE_START_KEYWORDS.stream()
                .filter(CypherTokenTypes.CLAUSE_CONTINUATION_KEYWORDS::contains)
                .collect(Collectors.toSet());
        assertTrue(intersection.isEmpty(),
                "CLAUSE_START and CLAUSE_CONTINUATION must be disjoint, overlap: " + intersection);
    }

    // --- Helper ---

    private List<Token> lex(String source) {
        Lexer lexer = new CypherLexer();
        lexer.start(source, 0, source.length(), 0);
        List<Token> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            if (lexer.getTokenType() != TokenType.WHITE_SPACE) {
                tokens.add(new Token(lexer.getTokenType(),
                        source.substring(lexer.getTokenStart(), lexer.getTokenEnd())));
            }
            lexer.advance();
        }
        return tokens;
    }

    private record Token(IElementType type, String text) {
    }
}
