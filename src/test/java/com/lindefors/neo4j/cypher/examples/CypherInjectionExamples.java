package com.lindefors.neo4j.cypher.examples;

import org.neo4j.driver.Session;

/**
 * Demonstrates which Java strings receive automatic Cypher language injection.
 */
final class CypherInjectionExamples {
    private static final String FIELD_QUERY = """
            MATCH (movie:Movie)
            RETURN movie.title
            """;

    void supportedCases(Session session) {
        // Direct Neo4j driver argument.
        session.run("MATCH (person:Person) RETURN person");

        // Local variable declared separately from the run call.
        String localQuery = """
                MATCH (person:Person)
                WHERE person.name = $name
                RETURN person
                """;
        session.run(localQuery);

        // A simple chain of resolvable local variables.
        String sourceQuery = "MATCH (person)-[:KNOWS]->(friend) RETURN friend";
        String selectedQuery = sourceQuery;
        session.run(selectedQuery);

        // A resolvable field or constant declared in the same Java file.
        session.run(FIELD_QUERY);
    }

    void deliberatelyNotInjected(Session session, String dynamicClause, boolean usePeople) {
        // Dynamically assembled strings are ambiguous and are not injected.
        String dynamicQuery = "MATCH (n) " + dynamicClause;
        session.run(dynamicQuery);

        // Expressions with multiple possible values are also not injected.
        String conditionalQuery = usePeople
                ? "MATCH (person:Person) RETURN person"
                : "MATCH (movie:Movie) RETURN movie";
        session.run(conditionalQuery);
    }
}
