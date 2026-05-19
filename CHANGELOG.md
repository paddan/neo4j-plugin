# Changelog

## [1.0.14] — 2026-05-19

### Fixed
- Pipe-separerade labels och relationship types (`Movie|Actor|Director`, `KNOWS|LIKES`) highlightas nu alla korrekt, inte bara den första
- Map-värden (`{ actor: node }`) highlightas inte längre felaktigt som node labels
- Labels inuti subquery-block (`CALL { }`, `EXISTS { }`) highlightas korrekt

## [1.0.13] — 2026-05-18

### Changed
- Build: changeNotes läses nu automatiskt från CHANGELOG.md; lade till `<category>Languages</category>` i plugin.xml för korrekt Marketplace-kategorisering

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
