# Neo4j Cypher Syntax Highlighter, Code Completion and Formatter (IntelliJ Plugin)

[![Latest Release](https://img.shields.io/gitea/v/release/paddan/neo4j-plugin?gitea_url=https%3A%2F%2Fcodeberg.org&label=Download&logo=codeberg)](https://codeberg.org/paddan/neo4j-plugin/releases/latest)

A lightweight IntelliJ IDEA plugin that adds support for Neo4j Cypher files (`.cyp`, `.cypher`).

## Features
- **Syntax highlighting** — keywords, functions, strings, comments, and more; customizable via `Settings > Editor > Color Scheme > Cypher`
- **Code completion** — Cypher keywords and the full Neo4j 5.x built-in function catalog
- **Formatter** — consistent indentation and spacing; auto-uppercases keywords on reformat (`Code > Reformat Code`)
- **Folding** — collapse multi-line delimiters and block comments
- **Structure view** — top-level clause keywords listed in the Structure panel (`View > Tool Windows > Structure`)
- **Comments** — `Ctrl+/` for line comments (`//`), `Ctrl+Shift+/` for block comments (`/* */`)
- **Brace matching** — highlights matching `()`, `[]`, `{}`
- **Error detection** — unclosed delimiters and unterminated string literals highlighted inline
- **Java language injection** — Cypher support inside string literals passed directly or through resolvable variables to Neo4j driver `run(...)` methods

## Requirements
- IntelliJ IDEA 2025.1+ (build 251+; no upper bound — compatible with future versions)
- JDK 21 (configured via Gradle toolchains)

## Build and Run
- Install dependencies with `asdf install` (if the asdf command is available)
- Compile & test: `./gradlew build`
- Run in sandbox IDE: `./gradlew runIde` (launches a test IDE with the plugin loaded)
- Package for distribution: `./gradlew buildPlugin` (ZIP appears in `build/distributions`)

## Install the Packaged Plugin
1) Build the ZIP with `./gradlew buildPlugin` (or download the latest release from Codeberg).
2) If you download the Actions artifact: there is a zip file `neo4j-plugin-<version>.zip` inside the downloaded `neo4j-plugin.zip` — install that one.
3) In IntelliJ IDEA: `Settings/Preferences > Plugins > ⚙ > Install Plugin from Disk...`.
4) Select the plugin ZIP (e.g., `build/distributions/neo4j-plugin-<version>.zip` or the downloaded release ZIP), install, and restart the IDE.

## Usage
- Open or create `.cyp` / `.cypher` files to get Cypher syntax highlighting, completion, and formatting.
- In Java files, Cypher is injected automatically into strings that flow into `org.neo4j.driver.*.run(...)`. Direct arguments, local variables, simple aliases, and fields declared in the same file are supported. Dynamically assembled strings are deliberately ignored.
- See [`CypherInjectionExamples.java`](src/test/java/com/lindefors/neo4j/cypher/examples/CypherInjectionExamples.java) for supported and unsupported Java examples.
- Run `Code > Reformat Code` (`Ctrl+Alt+L` / `Cmd+Option+L`) to format and uppercase keywords.
- Adjust colors under `Settings/Preferences > Editor > Color Scheme > Cypher`.

## Developing
- Use `./gradlew runIde` for rapid iteration in a sandbox.
- Update plugin metadata in `src/main/resources/META-INF/plugin.xml`.
- Java sources live in `src/main/java/com/lindefors/neo4j/cypher`.
