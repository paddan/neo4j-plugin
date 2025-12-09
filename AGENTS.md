# Repository Guidelines

## Project Structure & Module Organization
- IntelliJ plugin sources live in `src/main/java/com/lindefors/neo4j/cypher` (lexer, parser, formatter, syntax highlighting, color settings).
- Plugin metadata and registrations are in `src/main/resources/META-INF/plugin.xml`; keep extensions and since/until build numbers here.
- Tests belong in `src/test/java` with fixtures under `src/test/resources` (example: `test.cyp`). Generated build output lands in `build/` (look for plugin ZIPs in `build/distributions` and sandbox IDE files in `build/idea-sandbox`).
- The intellij plugin should support current and future versions of intellij

## Build, Test, and Development Commands
- `./gradlew build` — compiles and runs the full test suite.
- `./gradlew test` — runs JUnit 5 tests only (faster for local checks).
- `./gradlew runIde` — launches a sandbox IntelliJ with the plugin installed for manual verification.
- `./gradlew buildPlugin` — packages the distributable ZIP in `build/distributions`.
- `./gradlew clean` — removes build outputs when a fresh build is needed.

## Coding Style & Naming Conventions
- Java 17 with 4-space indentation; avoid tabs. Keep imports sorted and remove unused ones when touching files.
- Package path stays under `com.lindefors.neo4j.cypher`. Use PascalCase for classes, camelCase for methods/fields, and UPPER_SNAKE_CASE for constants.
- Prefix Cypher components consistently (e.g., `CypherLexer`, `CypherFormattingModelBuilder`) and keep token/type naming aligned with existing `CypherTokenTypes`.
- Update `plugin.xml` alongside code changes that add new extensions or settings to keep IDE registration accurate.

## Testing Guidelines
- JUnit 5 (Jupiter) is configured via the Gradle BOM; add tests in `src/test/java` and keep fixtures small in `src/test/resources`.
- Prefer focused tests per feature (lexer, parser, formatter). Name methods to describe behavior, e.g., `formatsSpacingAroundOperators`.
- Run `./gradlew test` before opening a PR; use `runIde` to manually validate syntax highlighting, formatting, and color settings when behavior changes.

## Commit & Pull Request Guidelines
- Use short, imperative commit subjects (observed history: “Fixed build error”, “Refine operator spacing”). Keep commits scoped to a single concern.
- PRs should include: a concise summary of changes, linked issues if applicable, screenshots/GIFs for UI-facing tweaks (color settings, formatting results), and the commands/tests executed.
- Note any compatibility changes affecting `sinceBuild`/`untilBuild` or packaging. Avoid committing IDE-local files; keep changes under versioned sources.

## Github action
- Compiles and packages a downloadable artefact that can be imported as a plugin in intellij

## Query structure and syntax for cypher queries
- All keywords should be upper case

### Read Query Structure
```
[USE]
[MATCH [WHERE]]
[OPTIONAL MATCH [WHERE]]
[WITH [ORDER BY] [SKIP] [LIMIT] [WHERE]]
RETURN [ORDER BY] [SKIP] [LIMIT]
```

### Write-Only Query Structure
```
[USE]
[CREATE]
[MERGE [ON CREATE ...] [ON MATCH ...]]
[WITH [ORDER BY] [SKIP] [LIMIT] [WHERE]]
[SET]
[DELETE]
[REMOVE]
[RETURN [ORDER BY] [SKIP] [LIMIT]]
```

### Read-Write Query Structure
```
[USE]
[MATCH [WHERE]]
[OPTIONAL MATCH [WHERE]]
[WITH [ORDER BY] [SKIP] [LIMIT] [WHERE]]
[CREATE]
[MERGE [ON CREATE ...] [ON MATCH ...]]
[WITH [ORDER BY] [SKIP] [LIMIT] [WHERE]]
[SET]
[DELETE]
[REMOVE]
[RETURN [ORDER BY] [SKIP] [LIMIT]]
```
