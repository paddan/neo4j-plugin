package com.lindefors.neo4j.cypher;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CypherFileType extends LanguageFileType {
    public static final CypherFileType INSTANCE = new CypherFileType();

    private CypherFileType() {
        super(CypherLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "Cypher";
    }

    @Override
    public @NotNull String getDescription() {
        return "Neo4j Cypher query file";
    }

    @Override
    public @NotNull @NonNls String getDefaultExtension() {
        return "cyp";
    }

    @Override
    public @Nullable Icon getIcon() {
        return null;
    }
}
