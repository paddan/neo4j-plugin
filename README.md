# Neo4j Cypher Syntax Highlighter and Formatter (IntelliJ Plugin)

[![Latest Release](https://img.shields.io/github/v/release/paddan/neo4j-plugin?label=Download&logo=github)](https://github.com/paddan/neo4j-plugin/releases/latest)

A lightweight IntelliJ IDEA plugin that adds basic support for Neo4j Cypher files (`.cyp`, `.cypher`): file type registration, syntax highlighting, a simple formatter, and editor color settings.

## Requirements
- IntelliJ IDEA 2024.2+ (IU; compatible up to 253.* per `untilBuild`)
- JDK 17 (configured via Gradle toolchains)

## Build and Run
- Install dependencies with `asdf install` (if the asdf command is available)
- Compile & test: `./gradlew build`
- Run in sandbox IDE: `./gradlew runIde` (launches a test IDE with the plugin loaded)
- Package for distribution: `./gradlew buildPlugin` (ZIP appears in `build/distributions`)

## Install the Packaged Plugin
1) Build the ZIP with `./gradlew buildPlugin` (or download the latest release from GitHub).
2) If you download the Actions artifact: There's a zip-file `neo4j-plugin-1.0.1.zip` inside the downloaded `neo4j-plugin.zip`, install that as a plugin in intellij.
3) In IntelliJ IDEA: `Settings/Preferences > Plugins > ⚙ > Install Plugin from Disk...`.
4) Select the plugin ZIP (e.g., `build/distributions/neo4j-plugin-1.0.1.zip` or the downloaded `neo4j-plugin-1.0.1.zip`), install, and restart the IDE.

## Usage
- Open or create `.cyp` / `.cypher` files to get Cypher syntax highlighting and formatting support.
- Adjust colors under `Settings/Preferences > Editor > Color Scheme > Cypher`.

## Developing
- Use `./gradlew runIde` for rapid iteration in a sandbox.
- Update plugin metadata in `src/main/resources/META-INF/plugin.xml`.
- Java sources live in `src/main/java/com/lindefors/neo4j/cypher`.
