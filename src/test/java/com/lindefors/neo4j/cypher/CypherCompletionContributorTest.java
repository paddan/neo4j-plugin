package com.lindefors.neo4j.cypher;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Integration tests for {@link CypherCompletionContributor} using the IntelliJ light platform fixture.
 * Tests exercise the contributor through real PSI + completion machinery.
 */
public class CypherCompletionContributorTest extends BasePlatformTestCase {

    private List<String> completions(String source) {
        myFixture.configureByText("test.cyp", source);
        LookupElement[] elements = myFixture.complete(CompletionType.BASIC);
        if (elements == null) return List.of();
        return Arrays.stream(elements).map(LookupElement::getLookupString).toList();
    }

    private Set<String> completionsSet(String source) {
        return Set.copyOf(completions(source));
    }

    public void testNoKeywordSuggestionsInsideNodePattern() {
        // Cursor inside (n:Pe<caret>) — should suppress keyword/function completions
        Set<String> result = completionsSet("MATCH (n:Pe<caret>)");
        // The completion list should not contain unrelated Cypher keywords like RETURN/WITH
        assertFalse("keywords should be suppressed inside node pattern: " + result,
                result.contains("RETURN") || result.contains("WITH"));
    }

    public void testNoKeywordSuggestionsInsideRelationshipPattern() {
        Set<String> result = completionsSet("MATCH (a)-[r:KN<caret>]->(b)");
        assertFalse("keywords should be suppressed inside relationship pattern: " + result,
                result.contains("RETURN") || result.contains("WITH"));
    }

    public void testKeywordSuggestionsOutsidePattern() {
        // After a clause boundary, RETURN should appear among completions.
        Set<String> result = completionsSet("MATCH (n) <caret>");
        assertTrue("RETURN should be suggested outside patterns: " + result,
                result.contains("RETURN"));
    }

    public void testIdentifierEarlierInSameValueClauseIsSuggested() {
        // Variables already mentioned in the current value clause should be offered for re-use.
        // Note: the contributor currently scopes to the *current* clause only — variables from
        // prior MATCH/WITH clauses are not propagated. This test locks in the current behavior;
        // the negative assertion below locks in the known limitation so a future fix can't
        // silently change it without updating the test.
        Set<String> result = completionsSet("MATCH (priorVar) RETURN someVar, <caret>");
        assertTrue("identifier from same RETURN clause should be in scope: " + result,
                result.contains("someVar"));
        assertFalse("variable from prior MATCH should NOT leak into RETURN scope (known limitation): "
                        + result,
                result.contains("priorVar"));
    }

    public void testBraceBlockBeforeCaretIsSkippedWhenCollectingIdentifiers() {
        // The {hidden: 1} map literal should not leak 'hidden' into completions outside it.
        Set<String> result = completionsSet("MATCH (n) WHERE n.x = {hidden: 1} RETURN someVar, <caret>");
        assertFalse("identifier inside a map literal should not leak: " + result,
                result.contains("hidden"));
        assertTrue("identifier in same clause should still be in scope: " + result,
                result.contains("someVar"));
    }

    public void testScopedCallSubqueryTreatsLabelPredicateIdentifierAsVisible() {
        Set<String> result = completionsSet("CALL (p) { RETURN p:Person, <caret> }");

        assertTrue("identifier in scoped CALL subquery should be suggested: " + result,
                result.contains("p"));
    }

    public void testNoCompletionInsideStringLiteral() {
        // Inside a string literal no Cypher keywords should be offered — use a caret position with
        // no prefix so a broken contributor would dump the full keyword set.
        Set<String> result = completionsSet("RETURN 'hello <caret> world'");
        assertFalse("no Cypher keywords should leak into string literals: " + result,
                result.contains("RETURN"));
        assertFalse("no Cypher keywords should leak into string literals: " + result,
                result.contains("MATCH"));
        assertFalse("no Cypher keywords should leak into string literals: " + result,
                result.contains("WHERE"));
    }

    public void testNoCompletionInsideBlockComment() {
        Set<String> result = completionsSet("/* trying <caret> here */ MATCH (n) RETURN n");
        assertFalse("no Cypher keywords should leak into block comments: " + result,
                result.contains("MATCH"));
        assertFalse("no Cypher keywords should leak into block comments: " + result,
                result.contains("RETURN"));
        assertFalse("no Cypher keywords should leak into block comments: " + result,
                result.contains("WHERE"));
    }
}
