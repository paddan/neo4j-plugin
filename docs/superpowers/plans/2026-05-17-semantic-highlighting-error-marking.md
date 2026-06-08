# Semantic Highlighting & Error Marking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Status: Completed.** This is a historical implementation plan and must not
> be executed again. The final implementation has evolved beyond the snippets
> below; use `CypherAnnotator` and `CypherAnnotatorTest` as the source of truth.

**Goal:** Add a `CypherAnnotator` that highlights node labels, relationship types, property keys, and function names in distinct colors, and marks unmatched delimiters and consecutive clause keywords as errors/warnings.

**Architecture:** A single `CypherAnnotator` fires only on the `CypherPsiFile` root, collects all leaf tokens into a list, and calls `computeAnnotations()` — a package-private method that runs one O(n) pass maintaining bracket-depth stacks and a clause-keyword state machine. This design keeps the PSI-coupling in `annotate()` and the logic in a plain, easily-testable method.

**Tech Stack:** IntelliJ Platform SDK (Annotator API, AnnotationHolder), JUnit 5

---

## File Map

| Action | File |
|--------|------|
| Modify | `src/main/java/com/lindefors/neo4j/cypher/CypherSyntaxHighlighter.java` |
| Modify | `src/main/java/com/lindefors/neo4j/cypher/CypherColorSettingsPage.java` |
| Create | `src/main/java/com/lindefors/neo4j/cypher/CypherAnnotator.java` |
| Modify | `src/main/resources/META-INF/plugin.xml` |
| Create | `src/test/java/com/lindefors/neo4j/cypher/CypherAnnotatorTest.java` |

---

### Task 1: Add semantic TextAttributesKey constants

**Files:**
- Modify: `src/main/java/com/lindefors/neo4j/cypher/CypherSyntaxHighlighter.java`

- [ ] **Step 1: Add four new constants after the PARAMETER constant**

Open `CypherSyntaxHighlighter.java` and add after the `PARAMETER` field (around line 35):

```java
    public static final TextAttributesKey LABEL =
            TextAttributesKey.createTextAttributesKey("CYPHER_LABEL", DefaultLanguageHighlighterColors.CLASS_NAME);
    public static final TextAttributesKey RELATIONSHIP_TYPE =
            TextAttributesKey.createTextAttributesKey("CYPHER_RELATIONSHIP_TYPE", DefaultLanguageHighlighterColors.INTERFACE_NAME);
    public static final TextAttributesKey PROPERTY_KEY =
            TextAttributesKey.createTextAttributesKey("CYPHER_PROPERTY_KEY", DefaultLanguageHighlighterColors.INSTANCE_FIELD);
    public static final TextAttributesKey FUNCTION_NAME =
            TextAttributesKey.createTextAttributesKey("CYPHER_FUNCTION_NAME", DefaultLanguageHighlighterColors.STATIC_METHOD);
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew compileJava
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/lindefors/neo4j/cypher/CypherSyntaxHighlighter.java
git commit -m "feat: add TextAttributesKey constants for semantic highlighting"
```

---

### Task 2: Write failing tests for CypherAnnotator semantic highlighting

**Files:**
- Create: `src/test/java/com/lindefors/neo4j/cypher/CypherAnnotatorTest.java`

- [ ] **Step 1: Create the test file**

```java
package com.lindefors.neo4j.cypher;

import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CypherAnnotatorTest {

    private final CypherAnnotator annotator = new CypherAnnotator();

    // --- helpers ---

    private List<CypherAnnotator.LeafToken> tokens(Object... pairs) {
        List<CypherAnnotator.LeafToken> result = new ArrayList<>();
        int offset = 0;
        for (int i = 0; i < pairs.length; i += 2) {
            IElementType type = (IElementType) pairs[i];
            String text = (String) pairs[i + 1];
            result.add(new CypherAnnotator.LeafToken(type, text, new TextRange(offset, offset + text.length())));
            offset += text.length();
        }
        return result;
    }

    // --- semantic highlighting ---

    @Test
    void identifierAfterColonOutsideAnythingIsLabel() {
        // :Person
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "Person"
        ));
        assertEquals(1, anns.size());
        assertEquals(CypherSyntaxHighlighter.LABEL, anns.get(0).attributes());
    }

    @Test
    void identifierAfterColonInsideBracketsIsRelationshipType() {
        // [:KNOWS]
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACKET_OPEN, "[",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.IDENTIFIER, "KNOWS",
                CypherTokenTypes.BRACKET_CLOSE, "]"
        ));
        assertEquals(1, anns.size());
        assertEquals(CypherSyntaxHighlighter.RELATIONSHIP_TYPE, anns.get(0).attributes());
    }

    @Test
    void identifierBeforeColonInsideBracesIsPropertyKey() {
        // {name: 'Alice'}
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACE_OPEN, "{",
                CypherTokenTypes.IDENTIFIER, "name",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.STRING, "'Alice'",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));
        assertEquals(1, anns.size());
        assertEquals(CypherSyntaxHighlighter.PROPERTY_KEY, anns.get(0).attributes());
    }

    @Test
    void identifierFollowedByParenIsFunctionName() {
        // count(
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.IDENTIFIER, "count",
                CypherTokenTypes.PAREN_OPEN, "("
        ));
        assertEquals(1, anns.size());
        assertEquals(CypherSyntaxHighlighter.FUNCTION_NAME, anns.get(0).attributes());
    }

    @Test
    void bareIdentifierGetsNoHighlight() {
        // n (plain variable, no context clues)
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.IDENTIFIER, "n"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void nestedPropertyKeysAreHighlighted() {
        // {a: {b: 1}} — both 'a' and 'b' are property keys
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACE_OPEN, "{",
                CypherTokenTypes.IDENTIFIER, "a",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.BRACE_OPEN, "{",
                CypherTokenTypes.IDENTIFIER, "b",
                CypherTokenTypes.COLON, ":",
                CypherTokenTypes.NUMBER, "1",
                CypherTokenTypes.BRACE_CLOSE, "}",
                CypherTokenTypes.BRACE_CLOSE, "}"
        ));
        assertEquals(2, anns.size());
        assertEquals(CypherSyntaxHighlighter.PROPERTY_KEY, anns.get(0).attributes());
        assertEquals(CypherSyntaxHighlighter.PROPERTY_KEY, anns.get(1).attributes());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew test --tests "com.lindefors.neo4j.cypher.CypherAnnotatorTest" 2>&1 | tail -15
```
Expected: compilation error — `CypherAnnotator` does not exist yet.

---

### Task 3: Implement CypherAnnotator (semantic highlighting)

**Files:**
- Create: `src/main/java/com/lindefors/neo4j/cypher/CypherAnnotator.java`

- [ ] **Step 1: Create CypherAnnotator.java**

```java
package com.lindefors.neo4j.cypher;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Applies semantic highlighting (node labels, relationship types, property keys, function names)
 * and flags structural errors (unmatched delimiters, consecutive clause keywords) in a single
 * pass over the flat token list.
 */
public class CypherAnnotator implements Annotator {

    private static final Set<String> VALID_CONSECUTIVE_PAIRS = Set.of(
            "OPTIONAL:MATCH",
            "DETACH:DELETE"
    );

    record LeafToken(IElementType type, String text, TextRange range) {}

    record Annotation(
            TextRange range,
            HighlightSeverity severity,
            @Nullable String message,
            @Nullable TextAttributesKey attributes) {

        static Annotation highlight(TextRange range, TextAttributesKey key) {
            return new Annotation(range, HighlightSeverity.INFORMATION, null, key);
        }

        static Annotation error(TextRange range, String message) {
            return new Annotation(range, HighlightSeverity.ERROR, message, null);
        }

        static Annotation warning(TextRange range, String message) {
            return new Annotation(range, HighlightSeverity.WARNING, message, null);
        }
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof CypherPsiFile)) return;

        for (Annotation ann : computeAnnotations(collectTokens(element))) {
            if (ann.attributes() != null) {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(ann.range())
                        .textAttributes(ann.attributes())
                        .create();
            } else {
                holder.newAnnotation(ann.severity(), ann.message())
                        .range(ann.range())
                        .create();
            }
        }
    }

    private List<LeafToken> collectTokens(PsiElement root) {
        List<LeafToken> result = new ArrayList<>();
        PsiElement child = root.getFirstChild();
        while (child != null) {
            result.add(new LeafToken(
                    child.getNode().getElementType(),
                    child.getText(),
                    child.getTextRange()));
            child = child.getNextSibling();
        }
        return result;
    }

    List<Annotation> computeAnnotations(List<LeafToken> tokens) {
        List<Annotation> result = new ArrayList<>();

        Deque<LeafToken> parenStack = new ArrayDeque<>();
        Deque<LeafToken> bracketStack = new ArrayDeque<>();
        Deque<LeafToken> braceStack = new ArrayDeque<>();

        String lastClauseKeyword = null;
        boolean seenContentAfterClause = false;

        for (int i = 0; i < tokens.size(); i++) {
            LeafToken tok = tokens.get(i);
            IElementType type = tok.type();

            // Bracket depth tracking (used for both semantic highlighting and error marking)
            if (type == CypherTokenTypes.PAREN_OPEN) {
                parenStack.push(tok);
            } else if (type == CypherTokenTypes.BRACKET_OPEN) {
                bracketStack.push(tok);
            } else if (type == CypherTokenTypes.BRACE_OPEN) {
                braceStack.push(tok);
            } else if (type == CypherTokenTypes.PAREN_CLOSE) {
                if (parenStack.isEmpty()) result.add(Annotation.error(tok.range(), "Unmatched ')'"));
                else parenStack.pop();
            } else if (type == CypherTokenTypes.BRACKET_CLOSE) {
                if (bracketStack.isEmpty()) result.add(Annotation.error(tok.range(), "Unmatched ']'"));
                else bracketStack.pop();
            } else if (type == CypherTokenTypes.BRACE_CLOSE) {
                if (braceStack.isEmpty()) result.add(Annotation.error(tok.range(), "Unmatched '}'"));
                else braceStack.pop();
            }

            // Semantic highlighting
            if (type == CypherTokenTypes.IDENTIFIER) {
                LeafToken prev = prevSignificant(tokens, i);
                LeafToken next = nextSignificant(tokens, i);

                if (prev != null && prev.type() == CypherTokenTypes.COLON) {
                    if (!bracketStack.isEmpty()) {
                        result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.RELATIONSHIP_TYPE));
                    } else {
                        result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.LABEL));
                    }
                } else if (next != null && next.type() == CypherTokenTypes.PAREN_OPEN) {
                    result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.FUNCTION_NAME));
                } else if (next != null && next.type() == CypherTokenTypes.COLON && !braceStack.isEmpty()) {
                    result.add(Annotation.highlight(tok.range(), CypherSyntaxHighlighter.PROPERTY_KEY));
                }
            }

            // Consecutive clause keyword detection
            if (type == CypherTokenTypes.KEYWORD) {
                String upper = tok.text().toUpperCase(Locale.ENGLISH);
                if (CypherTokenTypes.CLAUSE_START_KEYWORDS.contains(upper)) {
                    if (lastClauseKeyword != null && !seenContentAfterClause
                            && !VALID_CONSECUTIVE_PAIRS.contains(lastClauseKeyword + ":" + upper)) {
                        result.add(Annotation.warning(tok.range(), "Unexpected keyword, missing clause body"));
                    }
                    lastClauseKeyword = upper;
                    seenContentAfterClause = false;
                } else {
                    seenContentAfterClause = true;
                }
            } else if (type != TokenType.WHITE_SPACE) {
                seenContentAfterClause = true;
            }
        }

        // Unclosed delimiters
        for (LeafToken tok : parenStack) result.add(Annotation.error(tok.range(), "Unmatched '('"));
        for (LeafToken tok : bracketStack) result.add(Annotation.error(tok.range(), "Unmatched '['"));
        for (LeafToken tok : braceStack) result.add(Annotation.error(tok.range(), "Unmatched '{'"));

        return result;
    }

    private LeafToken prevSignificant(List<LeafToken> tokens, int i) {
        for (int j = i - 1; j >= 0; j--) {
            if (tokens.get(j).type() != TokenType.WHITE_SPACE) return tokens.get(j);
        }
        return null;
    }

    private LeafToken nextSignificant(List<LeafToken> tokens, int i) {
        for (int j = i + 1; j < tokens.size(); j++) {
            if (tokens.get(j).type() != TokenType.WHITE_SPACE) return tokens.get(j);
        }
        return null;
    }
}
```

- [ ] **Step 2: Run the semantic tests**

```bash
./gradlew test --tests "com.lindefors.neo4j.cypher.CypherAnnotatorTest" 2>&1 | tail -15
```
Expected: all 6 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/lindefors/neo4j/cypher/CypherAnnotator.java \
        src/test/java/com/lindefors/neo4j/cypher/CypherAnnotatorTest.java
git commit -m "feat: add CypherAnnotator with semantic highlighting"
```

---

### Task 4: Write failing tests for error marking

**Files:**
- Modify: `src/test/java/com/lindefors/neo4j/cypher/CypherAnnotatorTest.java`

- [ ] **Step 1: Add error-marking tests at the end of CypherAnnotatorTest**

Append these methods inside the class (before the closing `}`):

```java
    // --- error marking ---

    @Test
    void unmatchedOpenParenIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PAREN_OPEN, "("
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.ERROR, anns.get(0).severity());
        assertEquals("Unmatched '('", anns.get(0).message());
    }

    @Test
    void unmatchedCloseParenIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PAREN_CLOSE, ")"
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.ERROR, anns.get(0).severity());
        assertEquals("Unmatched ')'", anns.get(0).message());
    }

    @Test
    void matchedParensProduceNoErrors() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.IDENTIFIER, "n",
                CypherTokenTypes.PAREN_CLOSE, ")"
        ));
        // only the function-name rule might fire — but "n" is followed by ")", not "("
        // so no annotations at all
        assertTrue(anns.isEmpty());
    }

    @Test
    void unmatchedOpenBracketIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACKET_OPEN, "["
        ));
        assertEquals(1, anns.size());
        assertEquals("Unmatched '['", anns.get(0).message());
    }

    @Test
    void unmatchedOpenBraceIsError() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.BRACE_OPEN, "{"
        ));
        assertEquals(1, anns.size());
        assertEquals("Unmatched '{'", anns.get(0).message());
    }

    @Test
    void consecutiveClauseKeywordsProducesWarning() {
        // MATCH RETURN — no content between clause starters
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "MATCH",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "RETURN"
        ));
        assertEquals(1, anns.size());
        assertEquals(HighlightSeverity.WARNING, anns.get(0).severity());
        assertTrue(anns.get(0).message().contains("missing clause body"));
    }

    @Test
    void optionalMatchDoesNotWarn() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "OPTIONAL",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "MATCH"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void detachDeleteDoesNotWarn() {
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "DETACH",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "DELETE"
        ));
        assertTrue(anns.isEmpty());
    }

    @Test
    void clauseWithContentThenNewClauseDoesNotWarn() {
        // MATCH (n) RETURN n — content between MATCH and RETURN
        var anns = annotator.computeAnnotations(tokens(
                CypherTokenTypes.KEYWORD, "MATCH",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.PAREN_OPEN, "(",
                CypherTokenTypes.IDENTIFIER, "n",
                CypherTokenTypes.PAREN_CLOSE, ")",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.KEYWORD, "RETURN",
                TokenType.WHITE_SPACE, " ",
                CypherTokenTypes.IDENTIFIER, "n"
        ));
        // No errors. Two IDENTIFIERs "n" — neither follows a colon or precedes "(", no highlight.
        assertTrue(anns.isEmpty());
    }
```

- [ ] **Step 2: Run to verify new tests pass (they should — error marking is already implemented)**

```bash
./gradlew test --tests "com.lindefors.neo4j.cypher.CypherAnnotatorTest" 2>&1 | tail -15
```
Expected: all tests PASS (the implementation from Task 3 already covers errors).

If any fail, revisit Task 3's implementation before continuing.

- [ ] **Step 3: Commit the tests**

```bash
git add src/test/java/com/lindefors/neo4j/cypher/CypherAnnotatorTest.java
git commit -m "test: add error marking tests for CypherAnnotator"
```

---

### Task 5: Wire up plugin.xml and update color settings page

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`
- Modify: `src/main/java/com/lindefors/neo4j/cypher/CypherColorSettingsPage.java`

- [ ] **Step 1: Register annotator in plugin.xml**

Add this line inside `<extensions defaultExtensionNs="com.intellij">`, after the existing `<completion.contributor>` line:

```xml
        <annotator language="Cypher" implementationClass="com.lindefors.neo4j.cypher.CypherAnnotator"/>
```

- [ ] **Step 2: Update CypherColorSettingsPage**

Replace the entire file content:

```java
package com.lindefors.neo4j.cypher;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

/**
 * Exposes Cypher syntax elements to the IDE color settings UI with a short demo snippet.
 */
public class CypherColorSettingsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] DESCRIPTORS = new AttributesDescriptor[]{
            new AttributesDescriptor("Keyword", CypherSyntaxHighlighter.KEYWORD),
            new AttributesDescriptor("Identifier", CypherSyntaxHighlighter.IDENTIFIER),
            new AttributesDescriptor("Number", CypherSyntaxHighlighter.NUMBER),
            new AttributesDescriptor("String", CypherSyntaxHighlighter.STRING),
            new AttributesDescriptor("Comment", CypherSyntaxHighlighter.COMMENT),
            new AttributesDescriptor("Operator", CypherSyntaxHighlighter.OPERATOR),
            new AttributesDescriptor("Parentheses", CypherSyntaxHighlighter.PARENTHESES),
            new AttributesDescriptor("Brackets", CypherSyntaxHighlighter.BRACKETS),
            new AttributesDescriptor("Braces", CypherSyntaxHighlighter.BRACES),
            new AttributesDescriptor("Dot", CypherSyntaxHighlighter.DOT),
            new AttributesDescriptor("Parameter", CypherSyntaxHighlighter.PARAMETER),
            new AttributesDescriptor("Semantic//Node label", CypherSyntaxHighlighter.LABEL),
            new AttributesDescriptor("Semantic//Relationship type", CypherSyntaxHighlighter.RELATIONSHIP_TYPE),
            new AttributesDescriptor("Semantic//Property key", CypherSyntaxHighlighter.PROPERTY_KEY),
            new AttributesDescriptor("Semantic//Function name", CypherSyntaxHighlighter.FUNCTION_NAME),
    };

    @Override
    @Nullable
    public Icon getIcon() {
        return CypherFileType.INSTANCE.getIcon();
    }

    @Override
    @NotNull
    public SyntaxHighlighter getHighlighter() {
        return new CypherSyntaxHighlighter();
    }

    @Override
    @NotNull
    public String getDemoText() {
        return """
                // Sample Cypher
                MATCH (u:<label>User</label> {<propkey>id</propkey>: $userId})-[:<reltype>FRIEND</reltype>]->(friend)
                WHERE friend.active = true
                RETURN DISTINCT <funcname>count</funcname>(friend.name) AS total
                ORDER BY total DESC LIMIT 10;
                """;
    }

    @Override
    @Nullable
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return Map.of(
                "label", CypherSyntaxHighlighter.LABEL,
                "reltype", CypherSyntaxHighlighter.RELATIONSHIP_TYPE,
                "propkey", CypherSyntaxHighlighter.PROPERTY_KEY,
                "funcname", CypherSyntaxHighlighter.FUNCTION_NAME
        );
    }

    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return DESCRIPTORS;
    }

    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Cypher";
    }
}
```

- [ ] **Step 3: Run full test suite**

```bash
./gradlew test 2>&1 | tail -10
```
Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/META-INF/plugin.xml \
        src/main/java/com/lindefors/neo4j/cypher/CypherColorSettingsPage.java
git commit -m "feat: register CypherAnnotator and update color settings page"
```
