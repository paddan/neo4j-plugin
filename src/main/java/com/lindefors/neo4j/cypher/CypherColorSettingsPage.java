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
            new AttributesDescriptor("Identifier", CypherSyntaxHighlighter.IDENTIFIER),
            new AttributesDescriptor("Number", CypherSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("String", CypherSyntaxHighlighter.STRING),
            new AttributesDescriptor("Comment", CypherSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Operator", CypherSyntaxHighlighter.OPERATOR),
            new AttributesDescriptor("Parentheses", CypherSyntaxHighlighter.PARENTHESES),
            new AttributesDescriptor("Brackets", CypherSyntaxHighlighter.BRACKETS),
            new AttributesDescriptor("Braces", CypherSyntaxHighlighter.BRACES),
            new AttributesDescriptor("Dot", CypherSyntaxHighlighter.DOT)
    };

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }

    @Override
    public @NotNull SyntaxHighlighter getHighlighter() {
        return new CypherSyntaxHighlighter();
    }

    @Override
    public @NotNull String getDemoText() {
        return "" +
                "// Sample Cypher\n" +
                "MATCH (u:User {id: 42})-[:FRIEND]->(friend)\n" +
                "WHERE friend.active = true\n" +
                "RETURN DISTINCT friend.name, friend.age ORDER BY friend.age DESC LIMIT 10;\n";
    }

    @Override
    public @Nullable Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @Override
    public @NotNull AttributesDescriptor[] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public @NotNull ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Cypher";
    }
}
