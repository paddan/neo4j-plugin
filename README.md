# Neo4j Cypher Tools (IntelliJ Plugin)

A lightweight IntelliJ IDEA plugin that adds basic support for Neo4j Cypher files (`.cyp`, `.cypher`): file type registration, syntax highlighting, a simple formatter, and editor color settings.

## Requirements
- IntelliJ IDEA 2024.2+ (IU; compatible up to 253.* per `untilBuild`)
- JDK 21 (configured via Gradle toolchains)

## Build and Run
- Compile & test: `./gradlew build`
- Run in sandbox IDE: `./gradlew runIde` (launches a test IDE with the plugin loaded)
- Package for distribution: `./gradlew buildPlugin` (ZIP appears in `build/distributions`)

## Install the Packaged Plugin
1) Build the ZIP with `./gradlew buildPlugin` (or download the GitHub Actions artifact).
2) If you download the Actions artifact: install the downloaded `neo4j-plugin.zip` directly via “Install Plugin from Disk…” (no inner zip expected).
3) In IntelliJ IDEA: `Settings/Preferences > Plugins > ⚙ > Install Plugin from Disk...`.
4) Select the plugin ZIP (e.g., `build/distributions/neo4j-plugin-0.1.0.zip`), install, and restart the IDE.

## Usage
- Open or create `.cyp` / `.cypher` files to get Cypher syntax highlighting and formatting support.
- Adjust colors under `Settings/Preferences > Editor > Color Scheme > Cypher`.

## Developing
- Use `./gradlew runIde` for rapid iteration in a sandbox.
- Update plugin metadata in `src/main/resources/META-INF/plugin.xml`.
- Java sources live in `src/main/java/com/lindefors/neo4j/cypher`.
