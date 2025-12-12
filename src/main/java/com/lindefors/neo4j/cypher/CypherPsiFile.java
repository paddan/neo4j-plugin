package com.lindefors.neo4j.cypher;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PSI wrapper for Cypher files that wires the file type to the language.
 */
public class CypherPsiFile extends PsiFileBase {
    public CypherPsiFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, CypherLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return CypherFileType.INSTANCE;
    }

    @Override
    public @Nullable String toString() {
        return "Cypher File";
    }
}
