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
        CommonCodeStyleSettings.IndentOptions indentOptions = resolveIndentOptions(settings);
        int indentSize = resolveIndentSize(indentOptions);
        boolean useTabs = indentOptions != null && indentOptions.USE_TAB_CHARACTER;

        int rightMargin = settings.getRightMargin(CypherLanguage.INSTANCE);
        ASTNode node = element.getNode();
        CypherBlock.GroupLayout groupLayout = CypherBlock.GroupLayout.forRoot(node, rightMargin);
        Block block = new CypherBlock(node, Wrap.createWrap(WrapType.NONE, false),
                null, CypherIndents.none(), spacingBuilder, indentSize, useTabs, groupLayout);
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
                .before(CypherTokenTypes.SEMICOLON).spaces(0);
    }

    /**
     * Tries language-specific indent options first, then falls back to the project-wide "Other" settings.
     * This ensures Cypher files follow the same indent size as other file types when no Cypher-specific
     * settings have been configured.
     */
    private CommonCodeStyleSettings.IndentOptions resolveIndentOptions(CodeStyleSettings settings) {
        CommonCodeStyleSettings langSettings = settings.getCommonSettings(CypherLanguage.INSTANCE);
        CommonCodeStyleSettings.IndentOptions langIndent = langSettings.getIndentOptions();
        if (langIndent != null && langIndent.INDENT_SIZE > 0) {
            return langIndent;
        }
        return settings.OTHER_INDENT_OPTIONS;
    }

    private int resolveIndentSize(CommonCodeStyleSettings.IndentOptions indentOptions) {
        if (indentOptions != null && indentOptions.INDENT_SIZE > 0) {
            return indentOptions.INDENT_SIZE;
        }
        return 4;
    }
}
