package com.lindefors.neo4j.cypher;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Model for the Cypher structure view. The root element wraps the whole file; its children are the
 * top-level clause keywords found in the file.
 */
public class CypherStructureViewModel extends StructureViewModelBase
        implements StructureViewModel.ElementInfoProvider {

    public CypherStructureViewModel(@NotNull PsiFile psiFile, @Nullable Editor editor) {
        super(psiFile, editor, new CypherStructureViewElement(psiFile));
    }

    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }

    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        return !(element.getValue() instanceof PsiFile);
    }
}
