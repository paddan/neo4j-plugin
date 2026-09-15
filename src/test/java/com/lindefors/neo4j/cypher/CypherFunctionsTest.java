package com.lindefors.neo4j.cypher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CypherFunctionsTest {
    @Test
    void spatialFunctionsUseNeo4jQualifiedNames() {
        assertTrue(CypherFunctions.FUNCTIONS.contains("point.distance()"));
        assertTrue(CypherFunctions.FUNCTIONS.contains("point.withinBBox()"));
    }
}
