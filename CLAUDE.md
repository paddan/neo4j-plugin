# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language

All code, comments, commit messages, and documentation in this project must be written in English.

## Commands

```bash
./gradlew test                          # Run all tests
./gradlew test --tests "com.lindefors.neo4j.cypher.SomeTest.methodName"  # Run single test
./gradlew build                         # Compile + test
./gradlew runIde                        # Launch sandboxed IDE with the plugin loaded
./gradlew buildPlugin                   # Package into ZIP (build/distributions/)
```

## Architecture

This is an IntelliJ Platform plugin for Neo4j Cypher (`.cyp`, `.cypher` files). All Java source lives under `src/main/java/com/lindefors/neo4j/cypher/`.

### Pipeline: file open → display

1. `CypherFileType` recognises `.cyp`/`.cypher`
2. `CypherParserDefinition` wires together lexer + parser for IntelliJ
3. `CypherLexer` tokenises character-by-character (hand-written, no grammar file) → token stream
4. `CypherParser` consumes the token stream and builds a **flat PSI tree** — all tokens are direct children of `CypherPsiFile`, no nested nodes
5. `CypherSyntaxHighlighter` paints basic token colours (keywords, strings, numbers, operators, …)
6. `CypherAnnotator` does a second pass over the flat token list for **semantic highlighting** (labels, relationship types, property keys, function names) and structural error marking (unmatched delimiters, consecutive clause keywords)

### Key design decisions

**Flat AST.** `CypherParser` deliberately builds a flat tree rather than a grammar-driven parse tree. This keeps the implementation simple; all analysis (completion, folding, structure view) works by scanning the flat token list directly.

**Hand-written lexer.** `CypherLexer` is a ~400-line character scanner with no grammar tooling. Token precedence order: comments/strings → numbers → parameters → identifiers (keyword check) → punctuation → operators. Dual-purpose words (ALL, ANY, COUNT, EXISTS, POINT, RANGE, REPLACE) are emitted as IDENTIFIER when followed by `(`, otherwise as KEYWORD.

**Semantic highlighting in `CypherAnnotator`.** The annotator walks tokens with three depth-tracking stacks (`parenStack`, `bracketStack`, `braceStack`). A parallel `braceIsMapStack` (Boolean deque) distinguishes map literals `{}` from subquery blocks `CALL {}` / `EXISTS {}` by checking the keyword that precedes `{`. This is required so map values aren't mistakenly coloured as labels.

Label/reltype detection uses `isInLabelContext()` which walks backward through `(IDENTIFIER |)*` chains to find a root `:`, enabling pipe-separated multi-label patterns like `(n:Movie|Actor|Director)` to fully highlight.

**Completion heuristics.** `CypherCompletionContributor` uses backward token scanning to detect graph pattern context and suppress noisy suggestions inside `()` / `[]` node/relationship patterns.

### Other components

| Class | Purpose |
|-------|---------|
| `CypherFormattingModelBuilder` + `CypherBlock` | Reformatting (Ctrl+Alt+L), brace-depth indentation |
| `CypherKeywordCasePostFormatProcessor` | Auto-uppercases keywords after formatting |
| `CypherFoldingBuilder` | Folding regions for block comments and multi-line delimiters |
| `CypherBraceMatcher` | Highlight matching `()` `[]` `{}` |
| `CypherStructureViewFactory` | Structure panel showing top-level clauses |
| `CypherCommenter` | Ctrl+/ and Ctrl+Shift+/ comment actions |
| `CypherFunctions` | Catalogue of Neo4j 5.x built-ins for completion |

Plugin metadata (extensions, change notes) lives in `src/main/resources/META-INF/plugin.xml`. Change notes are generated automatically from `CHANGELOG.md` at build time.
