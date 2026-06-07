# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

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

## Testing conventions

Two styles coexist on purpose:

- **Unit tests** (lexer, annotator, formatter helpers) use **JUnit 5** — `@Test`-annotated methods on a plain class. These exercise pure logic without the IntelliJ test fixture.
- **Integration tests** that need the IntelliJ platform fixture (`CypherCompletionContributorTest`, `CypherKeywordCasePostFormatProcessorTest`) extend `BasePlatformTestCase`, which is built on **JUnit 3**. Methods must therefore be named `testXxx` (no `@Test` annotation). The `junit-vintage-engine` dependency in `build.gradle.kts` is what lets the JUnit 5 runner discover these.

Pick the style based on what you need from the IntelliJ runtime: if you can stub the input, use JUnit 5; if you need real PSI/completion/formatting, extend `BasePlatformTestCase`.

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

**Hand-written lexer.** `CypherLexer` is a ~400-line character scanner with no grammar tooling. Token precedence order: comments/strings → numbers → parameters → identifiers (keyword check) → punctuation → operators. Dual-purpose words (ALL, ANY, COLLECT, COUNT, EXISTS, POINT, RANGE, REPLACE) are emitted as IDENTIFIER when followed by `(`, otherwise as KEYWORD.

**Semantic highlighting in `CypherAnnotator`.** The annotator walks tokens with a shared delimiter stack so mismatched and crossed delimiters are detected correctly. A parallel `braceIsMapStack` (Boolean deque) distinguishes map literals `{}` from subquery blocks such as `CALL {}`, `CALL () {}`, `EXISTS {}`, `COUNT {}`, and `COLLECT {}`. This is required so map values aren't mistakenly coloured as labels.

Label/reltype detection uses `isInLabelContext()` which walks backward through `(IDENTIFIER |)*` chains to find a root `:`, enabling pipe-separated multi-label patterns like `(n:Movie|Actor|Director)` to fully highlight.

**Completion heuristics.** `CypherCompletionContributor` uses backward token scanning to detect graph pattern context and suppress noisy suggestions inside `()` / `[]` node/relationship patterns.

**Java language injection.** `CypherJavaLanguageInjector` is registered through the optional `cypher-java.xml` descriptor. It injects Cypher into Java string literals that flow directly or through resolvable variables to `org.neo4j.driver.*.run(...)`. When the Neo4j driver is absent from the project classpath, an explicit `org.neo4j.driver` import provides a conservative resolution fallback. Analysis is deliberately limited to the containing Java file and rejects dynamically assembled or reassigned values.

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
