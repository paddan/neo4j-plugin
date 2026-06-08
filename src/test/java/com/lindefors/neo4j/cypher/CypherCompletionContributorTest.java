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

    public void testIdentifiersFromEarlierClausesAreSuggested() {
        Set<String> result = completionsSet("MATCH (priorVar) RETURN someVar, <caret>");
        assertTrue("identifier from same RETURN clause should be in scope: " + result,
                result.contains("someVar"));
        assertTrue("variable from prior MATCH should be in scope: " + result,
                result.contains("priorVar"));
    }

    public void testWithClauseStopsIdentifiersFromEarlierScopesLeaking() {
        Set<String> result = completionsSet("MATCH (hidden) WITH 1 AS visible RETURN <caret>");

        assertTrue("WITH alias should be in scope: " + result, result.contains("visible"));
        assertFalse("identifier before WITH should not leak: " + result, result.contains("hidden"));
    }

    public void testWithClauseKeepsBareProjectedIdentifier() {
        Set<String> result = completionsSet("MATCH (kept) WITH kept RETURN <caret>");

        assertTrue("bare WITH projection should stay in scope: " + result, result.contains("kept"));
    }

    public void testWithExpressionDoesNotKeepItsSourceIdentifier() {
        Set<String> result = completionsSet("MATCH (hidden) WITH hidden.name RETURN <caret>");

        assertFalse("source identifier from a WITH expression should not leak: " + result,
                result.contains("hidden"));
    }

    public void testWithAliasRemainsVisibleAfterOrderBy() {
        Set<String> result = completionsSet("MATCH (hidden) WITH 1 AS visible ORDER BY visible RETURN <caret>");

        assertTrue("WITH alias should remain visible after ORDER BY: " + result, result.contains("visible"));
        assertFalse("identifier before WITH should not leak through ORDER BY: " + result,
                result.contains("hidden"));
    }

    public void testWithCountStarDoesNotPreserveEarlierVariables() {
        Set<String> result = completionsSet("MATCH (hidden) WITH count(*) AS total RETURN <caret>");

        assertTrue("WITH alias should be in scope: " + result, result.contains("total"));
        assertFalse("count(*) should not behave like WITH *: " + result, result.contains("hidden"));
    }

    public void testWithMultiplicationDoesNotPreserveEarlierVariables() {
        Set<String> result = completionsSet("MATCH (hidden) WITH hidden.score * 2 AS doubled RETURN <caret>");

        assertTrue("WITH alias should be in scope: " + result, result.contains("doubled"));
        assertFalse("multiplication should not behave like WITH *: " + result, result.contains("hidden"));
    }

    public void testWithWildcardPreservesEarlierVariables() {
        Set<String> result = completionsSet("MATCH (preserved) WITH * RETURN <caret>");

        assertTrue("WITH * should preserve earlier variables: " + result, result.contains("preserved"));
    }

    public void testUnionStopsIdentifiersFromPreviousBranchLeaking() {
        Set<String> result = completionsSet(
                "MATCH (previousBranch) RETURN previousBranch AS value "
                        + "UNION MATCH (currentBranch) RETURN <caret>");

        assertTrue("current UNION branch identifier should be in scope: " + result,
                result.contains("currentBranch"));
        assertFalse("previous UNION branch identifier should not leak: " + result,
                result.contains("previousBranch"));
    }

    public void testScopedCallImportsAreSuggestedBeforeFirstUse() {
        Set<String> result = completionsSet("MATCH (imported) CALL (imported) { RETURN <caret> }");

        assertTrue("CALL scope import should be in scope before its first use: " + result,
                result.contains("imported"));
    }

    public void testScopedCallWithoutImportsDoesNotSeeOuterIdentifier() {
        Set<String> result = completionsSet("MATCH (outer) CALL () { RETURN <caret> }");

        assertFalse("CALL () should not see outer-scope identifiers: " + result,
                result.contains("outer"));
    }

    public void testScopedCallWildcardImportsAllOuterIdentifiers() {
        Set<String> result = completionsSet("MATCH (outer) CALL (*) { RETURN <caret> }");

        assertTrue("CALL (*) should see outer-scope identifiers: " + result,
                result.contains("outer"));
    }

    public void testScopedCallImportSurvivesInnerWithClause() {
        Set<String> result = completionsSet(
                "MATCH (imported) CALL (imported) { WITH 1 AS inner RETURN <caret> }");

        assertTrue("CALL scope import should survive an inner WITH: " + result,
                result.contains("imported"));
        assertTrue("inner WITH alias should be in scope: " + result, result.contains("inner"));
    }

    public void testExistsSubqueryCanSeeOuterIdentifierBeforeFirstUse() {
        Set<String> result = completionsSet("MATCH (outer) WHERE EXISTS { RETURN <caret> }");

        assertTrue("EXISTS subquery should see outer-scope identifiers: " + result,
                result.contains("outer"));
    }

    public void testExistsSubqueryOuterIdentifierSurvivesInnerWithClause() {
        Set<String> result = completionsSet(
                "MATCH (outer) WHERE EXISTS { WITH 1 AS inner RETURN <caret> }");

        assertTrue("EXISTS outer-scope identifier should survive an inner WITH: " + result,
                result.contains("outer"));
        assertTrue("inner WITH alias should be in scope: " + result, result.contains("inner"));
    }

    public void testCountSubqueryCanSeeOuterIdentifierBeforeFirstUse() {
        Set<String> result = completionsSet("MATCH (outer) RETURN COUNT { RETURN <caret> }");

        assertTrue("COUNT subquery should see outer-scope identifiers: " + result,
                result.contains("outer"));
    }

    public void testMapLiteralCanSeeOuterIdentifier() {
        Set<String> result = completionsSet("MATCH (outer) RETURN {value: <caret>}");

        assertTrue("map literal value should see outer-scope identifiers: " + result,
                result.contains("outer"));
    }

    public void testMapLiteralInsideExistsCanSeeOuterIdentifier() {
        Set<String> result = completionsSet(
                "MATCH (outer) WHERE EXISTS { RETURN {value: <caret>} }");

        assertTrue("map literal inside EXISTS should see outer-scope identifiers: " + result,
                result.contains("outer"));
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
