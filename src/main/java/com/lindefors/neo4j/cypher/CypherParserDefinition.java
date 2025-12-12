package com.lindefors.neo4j.cypher;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the lexer/parser pair for Cypher along with token sets that inform IntelliJ about
 * whitespace, comments, and string literal handling.
 */
public class CypherParserDefinition implements ParserDefinition {
    private static final IFileElementType FILE = new IFileElementType(CypherLanguage.INSTANCE);
    private static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    private static final TokenSet COMMENTS = TokenSet.create(CypherTokenTypes.COMMENT);
    private static final TokenSet STRINGS = TokenSet.create(CypherTokenTypes.STRING);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new CypherLexer();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new CypherParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return STRINGS;
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new CypherPsiFile(viewProvider);
    }

    @Override
    public @NotNull PsiElement createElement(@NotNull ASTNode node) {
        return new com.intellij.extapi.psi.ASTWrapperPsiElement(node);
    }

    @Override
    public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
        return SpaceRequirements.MAY;
    }
}
