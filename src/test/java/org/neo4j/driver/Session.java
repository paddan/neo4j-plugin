package org.neo4j.driver;

/**
 * Minimal stub used so example sources in {@code com.lindefors.neo4j.cypher.examples}
 * compile against the test classpath without pulling in the Neo4j Java driver.
 */
public interface Session {
    Object run(String query);
}
