# Changelog

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
