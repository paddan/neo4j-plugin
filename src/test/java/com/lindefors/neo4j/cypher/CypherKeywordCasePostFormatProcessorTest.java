package com.lindefors.neo4j.cypher;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Integration tests for {@link CypherKeywordCasePostFormatProcessor}: verifies that reformatting
 * uppercases keywords without altering identifiers, strings, or comments.
 */
public class CypherKeywordCasePostFormatProcessorTest extends BasePlatformTestCase {

    private String reformat(String source) {
        myFixture.configureByText("test.cyp", source);
        PsiFile file = myFixture.getFile();
        new ReformatCodeProcessor(getProject(), file, null, false).run();
        return myFixture.getEditor().getDocument().getText();
    }

    public void testLowercaseKeywordsAreUppercased() {
        String result = reformat("match (n) return n");
        assertTrue("'match' should be uppercased, got: " + result, result.contains("MATCH"));
        assertTrue("'return' should be uppercased, got: " + result, result.contains("RETURN"));
        assertFalse("lowercase 'match' should be gone: " + result, result.contains("match "));
    }

    public void testIdentifiersAreNotUppercased() {
        String result = reformat("MATCH (someVar) RETURN someVar");
        assertTrue("identifier 'someVar' should preserve case: " + result,
                result.contains("someVar"));
        assertFalse("identifier should not be uppercased: " + result, result.contains("SOMEVAR"));
    }

    public void testStringLiteralsAreNotTouched() {
        String result = reformat("RETURN 'match return where'");
        assertTrue("string contents should preserve case: " + result,
                result.contains("'match return where'"));
    }

    public void testLineCommentsAreNotTouched() {
        String result = reformat("MATCH (n) // match this\nRETURN n");
        assertTrue("comment contents should preserve case: " + result,
                result.contains("// match this"));
    }

    public void testMixedCaseKeywordsAreNormalised() {
        String result = reformat("Match (n) ReTuRn n");
        assertTrue("'Match' should become 'MATCH': " + result, result.contains("MATCH"));
        assertTrue("'ReTuRn' should become 'RETURN': " + result, result.contains("RETURN"));
    }

    public void testSubqueryLabelsAndRelationshipTypesKeepTightColonSpacing() {
        String result = reformat("""
                MATCH (person)
                WHERE EXISTS {
                    MATCH (person:Person)-[:KNOWS]->(friend)
                    RETURN friend
                }
                RETURN person
                """);

        assertTrue("node labels inside subqueries should keep tight colon spacing: " + result,
                result.contains("person:Person"));
        assertTrue("relationship types inside subqueries should keep tight colon spacing: " + result,
                result.contains("[:KNOWS]"));
    }
}
