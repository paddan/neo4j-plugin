# Changelog

## [1.0.20] — 2026-06-08

### Fixed
- Improved Cypher token context handling

### Changed
- Codeberg release build no longer queries JetBrains Marketplace for the latest plugin version (`initializeIntellijPlatformPlugin` self-update check). The network-isolated runner timed out on that call, failing the build; it is now disabled so releases no longer depend on Marketplace reachability

## [1.0.19] — 2026-06-08

### Changed
- Codeberg release workflow no longer runs `verifyPlugin`; the shared runner consistently hit podman socket timeouts. Run the verifier locally before tagging instead

## [1.0.18] — 2026-06-08

### Changed
- Local Claude settings, generated agent worktrees, and their build artifacts are excluded from version control

### Documentation
- Added the completed semantic-highlighting design and implementation plan as historical documentation
- Updated the historical design to reflect the current ordered delimiter stack, map/subquery distinction, label-chain highlighting, and unterminated-literal detection

## [1.0.17] — 2026-06-07

### Added
- Java language injection: Cypher highlighting, completion, and error detection inside string literals flowing into `org.neo4j.driver.*.run(...)` — directly, through local variables, simple aliases, or fields declared in the same Java file. Dynamically assembled values are deliberately ignored
- Optional `cypher-java.xml` plugin descriptor gated on `com.intellij.modules.java`, so the plugin loads in IDEs without the Java module too

### Fixed
- Annotator no longer warns about "missing clause body" for `MERGE … ON CREATE SET` and `ON MATCH SET`
- Annotator now flags crossed delimiters (`( [ ) ]`) as errors via a single ordered delimiter stack
- Structure view only lists top-level clause keywords; keywords inside nested `()` / `[]` / `{}` are ignored
- Lexer correctly skips quoted strings, line comments, and block comments inside `$( … )` legacy parameters, so embedded parens don't break parameter scanning
- Lexer treats doubled backticks (` `` `) as escape sequences inside backtick-quoted identifiers

### Changed
- Annotator subquery-brace detection covers `COUNT { }` and scoped `CALL (var) { }` in addition to `CALL { }`, `EXISTS { }`, and `COLLECT { }`
- Completion contributor recognises scoped `CALL () { }` as a subquery block

### Documentation
- README, `CLAUDE.md`, and `AGENTS.md` document the Java injection behaviour and the new `cypher-java.xml` descriptor
- `examples/CypherInjectionExamples.java` shows supported and unsupported injection cases

### CI
- Forgejo and GitHub workflows run `./gradlew verifyPlugin` against the recommended IDE set
- GitHub build pipeline upgraded to JDK 21 to match the Gradle toolchain

### Tests
- New tests for the Java injector (resolved and unresolved driver classpath), structure view nesting, crossed delimiters, MERGE `ON SET`, and labels inside `COUNT { }` / scoped `CALL () { }` subquery blocks

## [1.0.16] — 2026-06-07

### Added
- Inline error detection for unterminated string literals, backtick-quoted identifiers, block comments, and `$( … )` parameter expressions

### Fixed
- Nested parentheses inside `$( … )` parameters (`$(foo(bar))`) are now lexed as a single parameter token
- Dual-purpose keyword functions (`EXISTS`, `COUNT`, `COLLECT`, …) followed by `(` across a newline are correctly treated as function calls rather than keywords
- Completion contributor now recognises `EXISTS { }` and `COLLECT { }` as subquery blocks in addition to `CALL { }`, keeping identifier scoping consistent with the annotator

### Changed
- Identifier keyword lookup avoids the `Locale`/double-allocation path with a dedicated ASCII upper-case helper
- Shared `SUBQUERY_KEYWORDS` set in `CypherTokenTypes` removes duplicate `CALL`/`EXISTS`/`COLLECT` lists between annotator and completion contributor

### Documentation
- README aligned with the JDK 21 Gradle toolchain and version-neutral installation instructions
- `CLAUDE.md` / `AGENTS.md` describe the split between JUnit 5 unit tests and JUnit 3 `BasePlatformTestCase` integration tests
- `CLAUSE_START_KEYWORDS` documents why `OPTIONAL` is treated as a clause head

### Tests
- 27 new tests covering the above, including BasePlatformTestCase-based integration tests for completion suppression and the keyword-case post-format processor (121 total, all green)

## [1.0.15] — 2026-05-20

### Documentation
- Translated remaining Swedish CHANGELOG entries to English
- Added CLAUDE.md with architecture overview and build commands

### Tests
- Added relationship-type highlighting test for full `MATCH (a)-[:REL]->(b)` pattern

## [1.0.14] — 2026-05-19

### Fixed
- Pipe-separated labels and relationship types (`Movie|Actor|Director`, `KNOWS|LIKES`) are now all highlighted, not just the first one
- Map values (`{ actor: node }`) are no longer incorrectly highlighted as node labels
- Labels inside subquery blocks (`CALL { }`, `EXISTS { }`) are highlighted correctly

## [1.0.13] — 2026-05-18

### Changed
- Build: change notes are now read automatically from CHANGELOG.md; added `<category>Languages</category>` to plugin.xml for correct Marketplace categorisation

## [1.0.12] — 2026-05-18

### Changed
- Updated JetBrains Marketplace description with full HTML overview of features

## [1.0.11] — 2026-05-18

### Added
- CypherAnnotator with semantic highlighting: keywords, function names, relationship types, labels, properties, and parameters rendered with distinct color categories
- Color settings page updated with semantic token categories
- TextAttributesKey constants for all semantic highlight types

### Fixed
- Indent formatter now uses `Indent.getSpaceIndent(indentSize)` explicitly for all space-based indentation, ensuring the configured indent size is respected instead of IntelliJ's language-default fallback (which produced 6-space indent for Cypher)
- Formatter reads indent size from `getCommonSettings(CypherLanguage)` with fallback to `OTHER_INDENT_OPTIONS`, so Cypher follows the project-wide "other file types" indent setting
- Unclosed-delimiter error detection restored after regression

## [1.0.10] — 2026-05-17

### Fixed
- Lexer now correctly disambiguates dual-purpose tokens (ALL, ANY, COUNT, EXISTS, POINT, RANGE, REPLACE): when followed by `(` they are emitted as identifiers (function calls), otherwise as keywords

### Tests
- Added comprehensive lexer tests covering keywords, case-insensitivity, comments, operators, and backtick identifiers
- Added folding builder tests covering block comments, parentheses, brackets, braces, and nested structures

## [1.0.9] — 2026-05-17

### Fixed
- String lexer now supports both backslash escape (`'Bob\'s'`) and doubled-quote escape (`'Bob''s'`); the doubled-quote form was accidentally broken in 1.0.8
- Removed `shortestPath()` and `allShortestPaths()` from function completions (these are path pattern expressions, not callable functions)
- Updated plugin description to reflect current feature set

### Tests
- Added regression tests for string escape styles, `FROM`/`EXISTS` keywords, hex literals (`0xFF`), and scientific notation (`1.5e10`)

## [1.0.8] — 2025-xx-xx

- Bug fixes and code quality improvements; see commit history for details.
