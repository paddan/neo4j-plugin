# Changelog

## [1.0.9] — 2026-05-17

### Fixed
- String lexer now supports both backslash escape (`'Bob\'s'`) and doubled-quote escape (`'Bob''s'`); the doubled-quote form was accidentally broken in 1.0.8
- Removed `shortestPath()` and `allShortestPaths()` from function completions (these are path pattern expressions, not callable functions)
- Updated plugin description to reflect current feature set

### Tests
- Added regression tests for string escape styles, `FROM`/`EXISTS` keywords, hex literals (`0xFF`), and scientific notation (`1.5e10`)

## [1.0.8] — 2025-xx-xx

- Bug fixes and code quality improvements; see commit history for details.
