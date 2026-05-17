# Changelog

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
