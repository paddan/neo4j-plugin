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
import org.jetbrains.annotations.NotNull;

public class CypherFormattingModelBuilder implements FormattingModelBuilder {
    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        PsiElement element = formattingContext.getPsiElement();
        CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        SpacingBuilder spacingBuilder = new SpacingBuilder(settings, CypherLanguage.INSTANCE)
                .around(CypherTokenTypes.OPERATOR).spaces(1)
                .after(CypherTokenTypes.COMMA).spaces(1)
                .before(CypherTokenTypes.COMMA).spaces(0)
                .before(CypherTokenTypes.PAREN_CLOSE).spaces(0)
                .after(CypherTokenTypes.PAREN_OPEN).spaces(0)
                .around(CypherTokenTypes.COLON).spaces(0)
                .before(CypherTokenTypes.SEMICOLON).spaces(0)
                .after(CypherTokenTypes.BRACE_OPEN).lineBreakInCode()
                .before(CypherTokenTypes.BRACE_CLOSE).lineBreakInCode();

        ASTNode node = element.getNode();
        Block block = new CypherBlock(node, Wrap.createWrap(WrapType.NONE, false),
                null, CypherIndents.none(), spacingBuilder);
        return FormattingModelProvider.createFormattingModelForPsiFile(element.getContainingFile(), block, settings);
    }
}
