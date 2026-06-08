package com.lindefors.neo4j.cypher;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CypherJavaLanguageInjectorUnresolvedTest extends BasePlatformTestCase {

    public void testInjectsWhenNeo4jDriverIsNotOnProjectClasspath() {
        myFixture.configureByText("CypherInjectionExamples.java", """
                import org.neo4j.driver.Session;

                final class CypherInjectionExamples {
                    void execute(Session session) {
                        String query = "MATCH (person:Person) RETURN person";
                        session.run(query);
                    }
                }
                """);

        PsiLiteralExpression literal = PsiTreeUtil.findChildrenOfType(
                        myFixture.getFile(), PsiLiteralExpression.class).stream()
                .filter(candidate -> "MATCH (person:Person) RETURN person".equals(candidate.getValue()))
                .findFirst()
                .orElseThrow();
        var injectedFiles = InjectedLanguageManager.getInstance(getProject()).getInjectedPsiFiles(literal);

        assertNotNull("Expected injection for an unresolved, explicitly imported Neo4j Session", injectedFiles);
        assertEquals(CypherLanguage.INSTANCE, injectedFiles.getFirst().first.getLanguage());
    }

    public void testDoesNotInjectForSimilarButUnrelatedPackageName() {
        myFixture.configureByText("Example.java", """
                import org.neo4j.driverfake.Session;

                final class Example {
                    void execute(Session session) {
                        session.run("MATCH (person:Person) RETURN person");
                    }
                }
                """);

        PsiLiteralExpression literal = PsiTreeUtil.findChildrenOfType(
                        myFixture.getFile(), PsiLiteralExpression.class).stream()
                .filter(candidate -> "MATCH (person:Person) RETURN person".equals(candidate.getValue()))
                .findFirst()
                .orElseThrow();

        assertNull(InjectedLanguageManager.getInstance(getProject()).getInjectedPsiFiles(literal));
    }

    public void testInjectsForNeo4jDriverWildcardImport() {
        myFixture.configureByText("Example.java", """
                import org.neo4j.driver.*;

                final class Example {
                    void execute(Session session) {
                        session.run("MATCH (person:Person) RETURN person");
                    }
                }
                """);

        PsiLiteralExpression literal = PsiTreeUtil.findChildrenOfType(
                        myFixture.getFile(), PsiLiteralExpression.class).stream()
                .filter(candidate -> "MATCH (person:Person) RETURN person".equals(candidate.getValue()))
                .findFirst()
                .orElseThrow();

        assertNotNull(InjectedLanguageManager.getInstance(getProject()).getInjectedPsiFiles(literal));
    }

    public void testDoesNotInjectForStaticNeo4jDriverWildcardImport() {
        myFixture.configureByText("Example.java", """
                import static org.neo4j.driver.Session.*;

                final class Example {
                    void execute(Session session) {
                        session.run("MATCH (person:Person) RETURN person");
                    }
                }
                """);

        PsiLiteralExpression literal = PsiTreeUtil.findChildrenOfType(
                        myFixture.getFile(), PsiLiteralExpression.class).stream()
                .filter(candidate -> "MATCH (person:Person) RETURN person".equals(candidate.getValue()))
                .findFirst()
                .orElseThrow();

        assertNull(InjectedLanguageManager.getInstance(getProject()).getInjectedPsiFiles(literal));
    }
}
