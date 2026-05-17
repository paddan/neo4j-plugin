package com.lindefors.neo4j.cypher;

import java.util.List;

/**
 * Catalogue of built-in Cypher functions offered as completion items. The list covers the Neo4j 5.x
 * function surface. Each entry is the canonical function name followed by {@code ()} so the completion
 * popup shows the call syntax immediately.
 */
public final class CypherFunctions {
    private CypherFunctions() {
    }

    public static final List<String> FUNCTIONS = List.of(
            // Scalar functions
            "coalesce()",
            "elementId()",
            "endNode()",
            "head()",
            "id()",
            "last()",
            "length()",
            "nullIf()",
            "properties()",
            "randomUUID()",
            "size()",
            "startNode()",
            "timestamp()",
            "toBoolean()",
            "toBooleanOrNull()",
            "toBooleanList()",
            "toFloat()",
            "toFloatOrNull()",
            "toFloatList()",
            "toInteger()",
            "toIntegerOrNull()",
            "toIntegerList()",
            "toString()",
            "toStringOrNull()",
            "toStringList()",
            "type()",
            "valueType()",

            // List functions
            "keys()",
            "labels()",
            "nodes()",
            "range()",
            "relationships()",
            "reverse()",
            "tail()",

            // Math functions
            "abs()",
            "ceil()",
            "e()",
            "exp()",
            "floor()",
            "isNaN()",
            "log()",
            "log10()",
            "pi()",
            "rand()",
            "round()",
            "sign()",
            "sqrt()",

            // Trigonometric functions
            "acos()",
            "asin()",
            "atan()",
            "atan2()",
            "cos()",
            "cot()",
            "degrees()",
            "haversin()",
            "radians()",
            "sin()",
            "tan()",

            // String functions
            "btrim()",
            "char()",
            "left()",
            "lTrim()",
            "normalize()",
            "replace()",
            "right()",
            "rTrim()",
            "split()",
            "substring()",
            "toLower()",
            "toUpper()",
            "trim()",

            // Temporal functions
            "date()",
            "datetime()",
            "duration()",
            "localdatetime()",
            "localtime()",
            "time()",

            // Spatial functions
            "distance()",
            "point()",
            "sphericalDistance()",
            "withinBBox()",

            // Vector functions
            "vector.similarity.cosine()",
            "vector.similarity.euclidean()",

            // Aggregating functions
            "avg()",
            "collect()",
            "count()",
            "max()",
            "min()",
            "percentileCont()",
            "percentileDisc()",
            "stDev()",
            "stDevP()",
            "sum()",

            // Predicate functions
            "all()",
            "any()",
            "exists()",
            "isEmpty()",
            "none()",
            "single()",

            // Reduce / list comprehension helpers
            "reduce()"
    );
}
