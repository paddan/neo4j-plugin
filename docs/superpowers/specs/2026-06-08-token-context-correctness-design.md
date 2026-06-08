# Token Context Correctness Design

## Goal

Fix the formatter, annotator, completion contributor, structure view, function
catalogue, and Java injector findings from the project review while leaving the
release workflow unchanged.

## Design

Add a small package-private `CypherTokenContext` utility for context rules that
are currently reimplemented inconsistently. It will classify brace pairs as map
literals or subquery blocks, identify relationship-pattern brackets, and
recognize compound clause heads.

The formatter and annotator will delegate their ambiguous delimiter decisions to
the shared utility. The structure view will suppress the second keyword in
compound clause heads. Completion will continue using its PSI scanning approach,
but will collect visible identifiers across preceding clauses until a statement
or scope boundary. The Java injector will require the exact
`org.neo4j.driver` namespace boundary. The Neo4j 5.x function catalogue and manual smoke
fixture will be corrected and expanded with documented Neo4j 5 functions.

## Testing

Each behavior change starts with a regression test:

- formatter preserves label/type colon spacing inside subqueries;
- list-comprehension label predicates are labels, not relationship types;
- identifiers introduced by preceding clauses appear in completion;
- compound clauses create one structure-view entry;
- the function catalogue contains documented vector functions and excludes the
  unsupported `sphericalDistance()`;
- unresolved imports outside `org.neo4j.driver.` do not trigger injection.

The final verification is a fresh `./gradlew build --rerun-tasks`.
