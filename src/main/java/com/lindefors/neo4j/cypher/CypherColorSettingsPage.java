package com.lindefors.neo4j.cypher;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class CypherColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Keyword", CypherSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Identifier / Variable", CypherSyntaxHighlighter.IDENTIFIER),
            new AttributesDescriptor("Number", CypherSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("String", CypherSyntaxHighlighter.STRING),
            new AttributesDescriptor("Comment", CypherSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Operator", CypherSyntaxHighlighter.OPERATOR),
            new AttributesDescriptor("Parentheses", CypherSyntaxHighlighter.PARENTHESES),
            new AttributesDescriptor("Brackets", CypherSyntaxHighlighter.BRACKETS),
            new AttributesDescriptor("Braces", CypherSyntaxHighlighter.BRACES),
            new AttributesDescriptor("Dot", CypherSyntaxHighlighter.DOT),
            new AttributesDescriptor("Parameter", CypherSyntaxHighlighter.PARAMETER)
    };

    @Override
    @Nullable
    public Icon getIcon() {
        return null;
    }

    @Override
    @NotNull
    public SyntaxHighlighter getHighlighter() {
        return new CypherSyntaxHighlighter();
    }

    @Override
    @NotNull
    public String getDemoText() {
        return """
                // Sample Cypher
                MATCH (u:User {id: $userId, name: $(userName)})-[:FRIEND]->(friend)
                WHERE friend.active = true
                CALL { WITH $userId RETURN COUNT(*) AS c }
                RETURN DISTINCT friend.name, friend.age, c ORDER BY friend.age DESC LIMIT 10;
                """;
    }

    @Override
    @Nullable
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override
    @NotNull
    public AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    @NotNull
    public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Cypher";
    }
}
