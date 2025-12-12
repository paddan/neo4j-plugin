package com.lindefors.neo4j.cypher;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import static com.lindefors.neo4j.cypher.CypherTokenTypes.KEYWORDS;

public class CypherCompletionContributor extends CompletionContributor {
    private static final List<String> OPERATORS = Arrays.asList(
            "=", "<>", "<", ">", "<=", ">=", "+", "-", "*", "/", "%", "^",
            "AND", "OR", "XOR", "NOT", "IN", "IS", "CONTAINS", "STARTS", "ENDS"
    );

    public CypherCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withLanguage(CypherLanguage.INSTANCE),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        PsiElement parent = position.getParent();
                        if (isInCommentOrString(position) || isInCommentOrString(parent)) {
                            return;
                        }

                        // Add keywords
                        for (String keyword : KEYWORDS) {
                            result.addElement(LookupElementBuilder.create(keyword)
                                    .withCaseSensitivity(false));
                        }

                        // Add operators
                        for (String operator : OPERATORS) {
                            result.addElement(LookupElementBuilder.create(operator)
                                    .withCaseSensitivity(false));
                        }
                    }
                });
    }

    private static boolean isInCommentOrString(@Nullable PsiElement element) {
        if (element == null || element.getNode() == null) {
            return false;
        }
        return element.getNode().getElementType() == CypherTokenTypes.COMMENT
                || element.getNode().getElementType() == CypherTokenTypes.STRING;
    }
}
