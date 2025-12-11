package com.lindefors.neo4j.cypher;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

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
}
