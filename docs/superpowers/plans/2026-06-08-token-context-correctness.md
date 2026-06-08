# Token Context Correctness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix review findings 2-7 while leaving the release workflow unchanged.

**Architecture:** Centralize ambiguous delimiter and compound-clause context in
`CypherTokenContext`. Keep completion scope collection and Java injection checks
within their existing components because those rules are component-specific.

**Tech Stack:** Java 21, IntelliJ Platform SDK, JUnit 5, JUnit Vintage.

---

### Task 1: Add regression tests

**Files:**
- Modify: `src/test/java/com/lindefors/neo4j/cypher/CypherKeywordCasePostFormatProcessorTest.java`
- Modify: `src/test/java/com/lindefors/neo4j/cypher/CypherAnnotatorTest.java`
- Modify: `src/test/java/com/lindefors/neo4j/cypher/CypherCompletionContributorTest.java`
- Modify: `src/test/java/com/lindefors/neo4j/cypher/CypherStructureViewElementTest.java`
- Modify: `src/test/java/com/lindefors/neo4j/cypher/CypherLexerComprehensiveTest.java`
- Modify: `src/test/java/com/lindefors/neo4j/cypher/CypherJavaLanguageInjectorUnresolvedTest.java`

- [x] Add one focused regression test for each reviewed behavior.
- [x] Run the focused tests and verify they fail for the expected reasons.

### Task 2: Introduce shared token context

**Files:**
- Create: `src/main/java/com/lindefors/neo4j/cypher/CypherTokenContext.java`
- Modify: `src/main/java/com/lindefors/neo4j/cypher/CypherBlock.java`
- Modify: `src/main/java/com/lindefors/neo4j/cypher/CypherAnnotator.java`
- Modify: `src/main/java/com/lindefors/neo4j/cypher/CypherStructureViewElement.java`

- [x] Implement brace, relationship-bracket, and compound-clause classification.
- [x] Delegate formatter, annotator, and structure-view decisions to it.
- [x] Run the focused formatter, annotator, and structure-view tests.

### Task 3: Fix component-specific behavior

**Files:**
- Modify: `src/main/java/com/lindefors/neo4j/cypher/CypherCompletionContributor.java`
- Modify: `src/main/java/com/lindefors/neo4j/cypher/CypherJavaLanguageInjector.java`
- Modify: `src/main/java/com/lindefors/neo4j/cypher/CypherFunctions.java`
- Modify: `src/test/resources/examples/test.cyp`

- [x] Collect identifiers across preceding clauses within the same scope.
- [x] Tighten the unresolved Neo4j import namespace check.
- [x] Correct and expand the Neo4j 5.x function catalogue and smoke fixture.
- [x] Run the focused completion, injector, and catalogue tests.

### Task 4: Verify

- [x] Run `./gradlew build --rerun-tasks`.
- [x] Confirm `.forgejo/workflows/release.yml` is unchanged.
- [x] Review `git diff --check` and the final diff.
