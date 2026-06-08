# Design: Semantic Highlighting & Error Marking

**Date:** 2026-05-17
**Status:** Implemented

> Historical design document. The implementation has since evolved beyond the
> original approach described below. See `CypherAnnotator` and its tests for the
> current behavior.

## Problem

The plugin currently highlights all identifiers with a single color regardless of their role in the query. Users cannot visually distinguish node labels, relationship types, property keys, and function calls. There is also no feedback for syntactic errors like unmatched brackets.

## Approach

Single-pass `CypherAnnotator` that fires once on the `CypherPsiFile` root. Walks all leaf tokens in order, maintaining an ordered delimiter stack, a map-vs-subquery brace stack, and a small state machine. Applies semantic highlighting and error markers in one O(n) pass.

No parser changes required. The existing flat AST is sufficient.

## Semantic Highlighting

Four new categories, each gets its own `TextAttributesKey` and color settings entry:

| Category | Token | Detection rule |
|---|---|---|
| Node label | IDENTIFIER | In a colon/pipe label chain outside map literals and relationship brackets |
| Relationship type | IDENTIFIER | In a colon/pipe label chain inside relationship brackets |
| Property key | IDENTIFIER | Followed by a colon inside a map literal |
| Function name | IDENTIFIER | nextSignificant == PAREN_OPEN |

"Significant" = skip whitespace tokens.

## Error Marking

Four rules; structural and literal errors use ERROR severity, while consecutive
clause keywords use WARNING severity:

1. **Unmatched open delimiter** — a `(`, `[`, or `{` with no matching close by EOF → error annotation on the opening token, message "Unmatched '('" etc.
2. **Unmatched or crossed close delimiter** — a `)`, `]`, or `}` that does not match the latest opening delimiter → error annotation on the closing token.
3. **Unterminated literal** — unterminated strings, backtick identifiers, block comments, and parameter expressions → error annotation on the token.
4. **Consecutive clause keywords** — a `CLAUSE_START_KEYWORD` immediately followed by another `CLAUSE_START_KEYWORD` (only whitespace between) → WARNING on the second keyword, message "Unexpected keyword, missing clause body".

## New Files

- `src/main/java/com/lindefors/neo4j/cypher/CypherAnnotator.java`

## Modified Files

- `CypherSyntaxHighlighter.java` — add `CYPHER_LABEL`, `CYPHER_RELATIONSHIP_TYPE`, `CYPHER_PROPERTY_KEY`, `CYPHER_FUNCTION_NAME` constants
- `CypherColorSettingsPage.java` — add the four new keys to the demo/settings page
- `plugin.xml` — register `<annotator language="Cypher" implementationClass="...CypherAnnotator"/>`

## Testing

- Unit tests via `CypherAnnotatorTest` using `StubAstNode` infrastructure (or direct token list)
- Cover: node label, rel-type, property key, function name, unmatched open, unmatched close, consecutive clause keywords
- Cover: nested structures (`{a: {b: 1}}` — inner `b` is property key, outer `a` is property key)
