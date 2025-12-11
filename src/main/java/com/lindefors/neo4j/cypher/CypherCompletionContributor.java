package com.lindefors.neo4j.cypher;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class CypherCompletionContributor extends CompletionContributor {
    private static final List<String> KEYWORDS = Arrays.asList(
            "MATCH", "OPTIONAL", "WHERE", "RETURN", "WITH", "UNWIND",
            "CREATE", "MERGE", "DELETE", "DETACH", "SET", "REMOVE",
            "FOREACH", "LOAD", "CSV", "FROM", "HEADERS", "CALL", "YIELD",
            "AS", "ORDER", "BY", "SKIP", "LIMIT", "ASC", "DESC",
            "UNION", "ALL", "DISTINCT", "ON", "USING", "INDEX", "CONSTRAINT",
            "EXISTS", "TRUE", "FALSE", "NULL", "COUNT"
    );

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
