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

/**
 * Exposes Cypher syntax elements to the IDE color settings UI with a short demo snippet.
 */
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
            new AttributesDescriptor("Dot", CypherSyntaxHighlighter.DOT),
            new AttributesDescriptor("Parameter", CypherSyntaxHighlighter.PARAMETER),
            new AttributesDescriptor("Semantic//Node label", CypherSyntaxHighlighter.LABEL),
            new AttributesDescriptor("Semantic//Relationship type", CypherSyntaxHighlighter.RELATIONSHIP_TYPE),
            new AttributesDescriptor("Semantic//Property key", CypherSyntaxHighlighter.PROPERTY_KEY),
            new AttributesDescriptor("Semantic//Function name", CypherSyntaxHighlighter.FUNCTION_NAME),
    };

    @Override
    @Nullable
    public Icon getIcon() {
        return CypherFileType.INSTANCE.getIcon();
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
                MATCH (u:<label>User</label> {<propkey>id</propkey>: $userId})-[:<reltype>FRIEND</reltype>]->(friend)
                WHERE friend.active = true
                RETURN DISTINCT <funcname>count</funcname>(friend.name) AS total
                ORDER BY total DESC LIMIT 10;
                """;
    }

    @Override
    @Nullable
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return Map.of(
                "label", CypherSyntaxHighlighter.LABEL,
                "reltype", CypherSyntaxHighlighter.RELATIONSHIP_TYPE,
                "propkey", CypherSyntaxHighlighter.PROPERTY_KEY,
                "funcname", CypherSyntaxHighlighter.FUNCTION_NAME
        );
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Cypher";
    }
}
