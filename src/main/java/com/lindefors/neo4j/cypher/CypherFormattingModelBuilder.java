package com.lindefors.neo4j.cypher;

import com.intellij.formatting.Block;
import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.FormattingModel;
import com.intellij.formatting.FormattingModelProvider;
import com.intellij.formatting.WrapType;
import com.intellij.formatting.SpacingBuilder;
import com.intellij.formatting.Wrap;
import com.intellij.lang.ASTNode;
import com.intellij.formatting.FormattingModelBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

/**
 * Builds IntelliJ formatting models for Cypher files using token-based spacing rules and simple
 * brace-depth indentation. The model delegates spacing to {@link CypherBlock} for pattern-aware tweaks.
 */
public class CypherFormattingModelBuilder implements FormattingModelBuilder {
    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        PsiElement element = formattingContext.getPsiElement();
        CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        SpacingBuilder spacingBuilder = createSpacingBuilder(settings);
        CommonCodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptions(element.getContainingFile().getFileType());
        int indentSize = resolveIndentSize(indentOptions);
        boolean useTabs = indentOptions.USE_TAB_CHARACTER;

        ASTNode node = element.getNode();
        Block block = new CypherBlock(node, Wrap.createWrap(WrapType.NONE, false),
                null, CypherIndents.none(), spacingBuilder, indentSize, useTabs);
        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), block, settings);
    }

    /**
     * Default spacing rules mirrored after Neo4j's reference style: operators spaced, punctuation tight.
     */
    static SpacingBuilder createSpacingBuilder(CodeStyleSettings settings) {
        return new SpacingBuilder(settings, CypherLanguage.INSTANCE)
                .around(CypherTokenTypes.OPERATOR).spaces(1)
                .after(CypherTokenTypes.COMMA).spaces(1)
                .before(CypherTokenTypes.COMMA).spaces(0)
                .before(CypherTokenTypes.PAREN_CLOSE).spaces(0)
                .after(CypherTokenTypes.PAREN_OPEN).spaces(0)
                .around(CypherTokenTypes.COLON).spaces(0)
                .before(CypherTokenTypes.SEMICOLON).spaces(0)
                .after(CypherTokenTypes.BRACE_OPEN).spaces(1)
                .before(CypherTokenTypes.BRACE_CLOSE).spaces(1);
    }

    /**
     * Uses IntelliJ indent settings when available, falling back to four spaces to match the bundled formatter.
     */
    private int resolveIndentSize(CommonCodeStyleSettings.IndentOptions indentOptions) {
        if (indentOptions != null && indentOptions.INDENT_SIZE > 0) {
            return indentOptions.INDENT_SIZE;
        }
        return 4;
    }
}
