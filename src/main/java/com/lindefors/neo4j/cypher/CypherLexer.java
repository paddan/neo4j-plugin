package com.lindefors.neo4j.cypher;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Minimal, hand-written lexer for Cypher that recognizes keywords, identifiers, parameters, comments,
 * strings, punctuation, and simple operators. The lexer is single-state because Cypher does not require
 * multi-state lexing for the subset the plugin supports.
 */
public class CypherLexer extends LexerBase {
    private static final Set<String> KEYWORDS = new HashSet<>(CypherTokenTypes.KEYWORDS);

    private CharSequence buffer = "";
    private int endOffset;
    private int position;
    private int tokenStart;
    private int tokenEnd;
    private IElementType tokenType;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer = buffer;
        this.endOffset = endOffset;
        this.position = startOffset;
        locateToken();
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public @Nullable IElementType getTokenType() {
        return tokenType;
    }

    @Override
    public int getTokenStart() {
        return tokenStart;
    }

    @Override
    public int getTokenEnd() {
        return tokenEnd;
    }

    @Override
    public void advance() {
        if (tokenType == null) {
            return;
        }
        position = tokenEnd;
        locateToken();
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

    /**
     * Advances the internal cursor to the next token. The implementation intentionally keeps the
     * precedence ordered (comments/strings → numbers → parameters → identifiers → punctuation → operators)
     * to avoid misclassifying shorter tokens when longer ones are possible.
     */
    private void locateToken() {
        if (skipWhitespace()) {
            return;
        }
        tokenStart = position;
        if (position >= endOffset) {
            tokenType = null;
            tokenEnd = position;
            return;
        }

        char current = buffer.charAt(position);

        if (isLineCommentStart(current)) {
            scanLineComment();
            return;
        }
        if (isBlockCommentStart(current)) {
            scanBlockComment();
            return;
        }
        if (current == '\'' || current == '\"') {
            scanString(current);
            return;
        }
        if (Character.isDigit(current)) {
            scanNumber();
            return;
        }
        if (current == '$') {
            scanParameter();
            return;
        }
        if (current == '{' && scanLegacyParameter()) {
            return;
        }
        if (isIdentifierStart(current) || current == '`') {
            scanIdentifier();
            return;
        }
        if (punctuationToken(current)) {
            return;
        }
        if (operatorChar(current)) {
            scanOperator();
            return;
        }

        tokenType = TokenType.BAD_CHARACTER;
        tokenEnd = ++position;
    }

    private boolean skipWhitespace() {
        int initial = position;
        while (position < endOffset) {
            char c = buffer.charAt(position);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                position++;
            } else {
                break;
            }
        }
        if (position > initial) {
            tokenStart = initial;
            tokenType = TokenType.WHITE_SPACE;
            tokenEnd = position;
            return true;
        }
        return false;
    }

    private boolean isLineCommentStart(char current) {
        return current == '/' && position + 1 < endOffset && buffer.charAt(position + 1) == '/';
    }

    private boolean isBlockCommentStart(char current) {
        return current == '/' && position + 1 < endOffset && buffer.charAt(position + 1) == '*';
    }

    private void scanLineComment() {
        position += 2;
        while (position < endOffset) {
            char c = buffer.charAt(position);
            if (c == '\n' || c == '\r') {
                break;
            }
            position++;
        }
        tokenType = CypherTokenTypes.COMMENT;
        tokenEnd = position;
    }

    private void scanBlockComment() {
        position += 2;
        while (position < endOffset) {
            if (buffer.charAt(position) == '*' && position + 1 < endOffset && buffer.charAt(position + 1) == '/') {
                position += 2;
                break;
            }
            position++;
        }
        tokenType = CypherTokenTypes.COMMENT;
        tokenEnd = position;
    }

    private void scanString(char quote) {
        position++;
        while (position < endOffset) {
            char c = buffer.charAt(position);
            if (c == '\\' && position + 1 < endOffset) {
                position += 2;
                continue;
            }
            position++;
            if (c == quote) {
                break;
            }
        }
        tokenType = CypherTokenTypes.STRING;
        tokenEnd = position;
    }

    private void scanNumber() {
        boolean seenDot = false;
        position++;
        while (position < endOffset) {
            char c = buffer.charAt(position);
            if (Character.isDigit(c)) {
                position++;
            } else if (c == '.' && !seenDot && position + 1 < endOffset && Character.isDigit(buffer.charAt(position + 1))) {
                seenDot = true;
                position++;
            } else {
                break;
            }
        }
        tokenType = CypherTokenTypes.NUMBER;
        tokenEnd = position;
    }

    /**
     * Lexes parameters in both {@code $name} and APOC-style {@code $(expression)} forms.
     */
    private void scanParameter() {
        position++; // consume $
        if (position < endOffset && buffer.charAt(position) == '(') {
            position++; // consume opening paren
            while (position < endOffset && buffer.charAt(position) != ')') {
                position++;
            }
            if (position < endOffset && buffer.charAt(position) == ')') {
                position++;
            }
            tokenType = CypherTokenTypes.PARAMETER;
            tokenEnd = position;
            return;
        }

        while (position < endOffset && isIdentifierPart(buffer.charAt(position))) {
            position++;
        }
        tokenType = CypherTokenTypes.PARAMETER;
        tokenEnd = position;
    }

    /**
     * Legacy parameter syntax uses {@code { name }}. This method only consumes the token when a full
     * identifier is wrapped in braces to avoid swallowing stray braces.
     */
    private boolean scanLegacyParameter() {
        int i = position + 1;
        while (i < endOffset && Character.isWhitespace(buffer.charAt(i))) {
            i++;
        }
        if (i >= endOffset || !isIdentifierStart(buffer.charAt(i))) {
            return false;
        }
        i++;
        while (i < endOffset && isIdentifierPart(buffer.charAt(i))) {
            i++;
        }
        while (i < endOffset && Character.isWhitespace(buffer.charAt(i))) {
            i++;
        }
        if (i >= endOffset || buffer.charAt(i) != '}') {
            return false;
        }

        position = i + 1;
        tokenType = CypherTokenTypes.PARAMETER;
        tokenEnd = position;
        return true;
    }

    /**
     * Reads identifiers and keywords, handling backtick-quoted identifiers and promoting matching
     * words to the {@link CypherTokenTypes#KEYWORD} token when appropriate.
     */
    private void scanIdentifier() {
        boolean quoted = buffer.charAt(position) == '`';
        position++;
        if (quoted) {
            while (position < endOffset) {
                char c = buffer.charAt(position++);
                if (c == '`') {
                    break;
                }
            }
            tokenType = CypherTokenTypes.IDENTIFIER;
            tokenEnd = position;
            return;
        }

        while (position < endOffset && isIdentifierPart(buffer.charAt(position))) {
            position++;
        }
        String word = buffer.subSequence(tokenStart, position).toString().toUpperCase(Locale.ENGLISH);
        tokenType = KEYWORDS.contains(word) ? CypherTokenTypes.KEYWORD : CypherTokenTypes.IDENTIFIER;
        tokenEnd = position;
    }

    private boolean punctuationToken(char current) {
        switch (current) {
            case '(':
                tokenType = CypherTokenTypes.PAREN_OPEN;
                break;
            case ')':
                tokenType = CypherTokenTypes.PAREN_CLOSE;
                break;
            case '[':
                tokenType = CypherTokenTypes.BRACKET_OPEN;
                break;
            case ']':
                tokenType = CypherTokenTypes.BRACKET_CLOSE;
                break;
            case '{':
                tokenType = CypherTokenTypes.BRACE_OPEN;
                break;
            case '}':
                tokenType = CypherTokenTypes.BRACE_CLOSE;
                break;
            case ',':
                tokenType = CypherTokenTypes.COMMA;
                break;
            case '.':
                tokenType = CypherTokenTypes.DOT;
                break;
            case ':':
                tokenType = CypherTokenTypes.COLON;
                break;
            case ';':
                tokenType = CypherTokenTypes.SEMICOLON;
                break;
            default:
                return false;
        }
        position++;
        tokenEnd = position;
        return true;
    }

    private void scanOperator() {
        position++;
        while (position < endOffset && operatorChar(buffer.charAt(position))) {
            position++;
        }
        tokenType = CypherTokenTypes.OPERATOR;
        tokenEnd = position;
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    private boolean operatorChar(char c) {
        return switch (c) {
            case '+', '-', '*', '/', '=', '<', '>', '&', '|', '!' -> true;
            default -> false;
        };
    }
}
