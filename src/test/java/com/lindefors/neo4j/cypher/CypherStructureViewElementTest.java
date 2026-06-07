package com.lindefors.neo4j.cypher;

import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.List;

public class CypherStructureViewElementTest extends BasePlatformTestCase {

    public void testOnlyTopLevelClausesAppearInStructureView() {
        myFixture.configureByText("test.cyp", """
                MATCH (outer)
                CALL {
                    MATCH (inner)
                    RETURN inner
                }
                RETURN outer
                """);

        TreeElement[] children = new CypherStructureViewElement(myFixture.getFile()).getChildren();
        List<String> presentations = Arrays.stream(children)
                .map(child -> child.getPresentation().getPresentableText())
                .toList();

        assertEquals(3, presentations.size());
        assertTrue(presentations.get(0).startsWith("MATCH"));
        assertTrue(presentations.get(1).startsWith("CALL"));
        assertTrue(presentations.get(2).startsWith("RETURN"));
    }
}
