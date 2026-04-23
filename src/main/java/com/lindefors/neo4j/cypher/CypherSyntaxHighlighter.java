package com.lindefors.neo4j.cypher;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Simple syntax highlighter that maps lexer token types to IntelliJ text attributes for Cypher.
 */
public class CypherSyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey KEYWORD =
            TextAttributesKey.createTextAttributesKey("CYPHER_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey IDENTIFIER =
            TextAttributesKey.createTextAttributesKey("CYPHER_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey NUMBER =
            TextAttributesKey.createTextAttributesKey("CYPHER_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey STRING =
            TextAttributesKey.createTextAttributesKey("CYPHER_STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey COMMENT =
            TextAttributesKey.createTextAttributesKey("CYPHER_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey OPERATOR =
            TextAttributesKey.createTextAttributesKey("CYPHER_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);
    public static final TextAttributesKey PARENTHESES =
            TextAttributesKey.createTextAttributesKey("CYPHER_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey BRACKETS =
            TextAttributesKey.createTextAttributesKey("CYPHER_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
    public static final TextAttributesKey BRACES =
            TextAttributesKey.createTextAttributesKey("CYPHER_BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey DOT =
            TextAttributesKey.createTextAttributesKey("CYPHER_DOT", DefaultLanguageHighlighterColors.DOT);
    public static final TextAttributesKey PARAMETER =
            TextAttributesKey.createTextAttributesKey("CYPHER_PARAMETER", DefaultLanguageHighlighterColors.INSTANCE_FIELD);

    private static final Map<IElementType, TextAttributesKey> KEYS = Map.ofEntries(
            Map.entry(CypherTokenTypes.KEYWORD, KEYWORD),
            Map.entry(CypherTokenTypes.IDENTIFIER, IDENTIFIER),
            Map.entry(CypherTokenTypes.NUMBER, NUMBER),
            Map.entry(CypherTokenTypes.STRING, STRING),
            Map.entry(CypherTokenTypes.COMMENT, COMMENT),
            Map.entry(CypherTokenTypes.OPERATOR, OPERATOR),
            Map.entry(CypherTokenTypes.PAREN_OPEN, PARENTHESES),
            Map.entry(CypherTokenTypes.PAREN_CLOSE, PARENTHESES),
            Map.entry(CypherTokenTypes.BRACKET_OPEN, BRACKETS),
            Map.entry(CypherTokenTypes.BRACKET_CLOSE, BRACKETS),
            Map.entry(CypherTokenTypes.BRACE_OPEN, BRACES),
            Map.entry(CypherTokenTypes.BRACE_CLOSE, BRACES),
            Map.entry(CypherTokenTypes.DOT, DOT),
            Map.entry(CypherTokenTypes.PARAMETER, PARAMETER)
    );

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new CypherLexer();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == TokenType.BAD_CHARACTER) {
            return pack(DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE);
        }
        TextAttributesKey key = KEYS.get(tokenType);
        return key == null ? TextAttributesKey.EMPTY_ARRAY : pack(key);
    }
}
