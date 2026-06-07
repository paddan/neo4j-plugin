package com.lindefors.neo4j.cypher;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class CypherJavaLanguageInjectorTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addFileToProject("org/neo4j/driver/Session.java", """
                package org.neo4j.driver;

                public interface Session {
                    Object run(String query);
                    Object run(String query, String metadata);
                }
                """);
    }

    public void testInjectsDirectNeo4jRunArgument() {
        configureJava("""
                import org.neo4j.driver.Session;

                class Example {
                    void execute(Session session) {
                        session.run("MATCH (n) RETURN n");
                    }
                }
                """);

        assertInjected("MATCH (n) RETURN n");
    }

    public void testInjectsResolvableLocalVariableAndAlias() {
        configureJava("""
                import org.neo4j.driver.Session;

                class Example {
                    void execute(Session session) {
                        String query = "MATCH (n:Person) RETURN n";
                        String selectedQuery = query;
                        session.run(selectedQuery);
                    }
                }
                """);

        assertInjected("MATCH (n:Person) RETURN n");
    }

    public void testInjectsResolvableFieldInSameFile() {
        configureJava("""
                import org.neo4j.driver.Session;

                class Example {
                    private static final String QUERY = "MATCH (n:Movie) RETURN n";

                    void execute(Session session) {
                        session.run(QUERY);
                    }
                }
                """);

        assertInjected("MATCH (n:Movie) RETURN n");
    }

    public void testDoesNotInjectDynamicOrUnrelatedStrings() {
        configureJava("""
                import org.neo4j.driver.Session;

                class Example {
                    void execute(Session session, String suffix) {
                        String dynamic = "MATCH (n)" + suffix;
                        session.run(dynamic);
                        String reassigned = "MATCH (changed) RETURN changed";
                        reassigned = suffix;
                        session.run(reassigned);
                        run("MATCH (other) RETURN other");
                    }

                    void run(String text) {}
                }
                """);

        assertNotInjected("MATCH (n)");
        assertNotInjected("MATCH (changed) RETURN changed");
        assertNotInjected("MATCH (other) RETURN other");
    }

    public void testDoesNotInjectLaterRunArguments() {
        configureJava("""
                import org.neo4j.driver.Session;

                class Example {
                    void execute(Session session) {
                        session.run("MATCH (n) RETURN n", "MATCH is only metadata here");
                    }
                }
                """);

        assertInjected("MATCH (n) RETURN n");
        assertNotInjected("MATCH is only metadata here");
    }

    private void configureJava(String source) {
        myFixture.configureByText("Example.java", source);
    }

    private void assertInjected(String value) {
        PsiLiteralExpression literal = findLiteral(value);
        var injectedFiles = InjectedLanguageManager.getInstance(getProject()).getInjectedPsiFiles(literal);
        assertNotNull("Expected Cypher injection for: " + value, injectedFiles);
        assertEquals(CypherLanguage.INSTANCE, injectedFiles.getFirst().first.getLanguage());
    }

    private void assertNotInjected(String value) {
        PsiLiteralExpression literal = findLiteral(value);
        assertNull(
                "Did not expect Cypher injection for: " + value,
                InjectedLanguageManager.getInstance(getProject()).getInjectedPsiFiles(literal));
    }

    private PsiLiteralExpression findLiteral(String value) {
        PsiFile file = myFixture.getFile();
        return PsiTreeUtil.findChildrenOfType(file, PsiLiteralExpression.class).stream()
                .filter(literal -> value.equals(literal.getValue()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Literal not found: " + value));
    }
}
