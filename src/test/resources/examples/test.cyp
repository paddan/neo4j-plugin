/*
  Manual smoke-test fixture for the Cypher plugin.
  Covers comments, highlighting, formatting, folding, brace matching,
  structure view, parameters, strings, functions, and newer keywords.
*/
// Clauses, identifiers, operators, strings, and both new parameter syntaxes.
PROFILE
MATCH (p:person)-[r:knows]->(friend:person)
WHERE p.name STARTS
WITH 'ma' AND p.nickname = 'bob''s'
AND p.id = $personid
AND friend.locale = $(locale)
RETURN p.name AS personname, r.since AS since, friend.name AS friendname ORDER BY since DESC, personname SKIP 1 LIMIT 5;

// optional match, legacy braced parameters, case/when/else, and function calls.
OPTIONAL MATCH (p:person)-[:acted_in]->(m:movie {id: {movieid}})
WHERE EXISTS(m.tagline) AND m.released >= 1999
RETURN p, m, CASE
  WHEN m.released >= 2020 THEN 'recent'
  WHEN m.released >= 2000 THEN 'modern'
  ELSE 'classic'

END AS era,
coalesce(m.tagline, 'n/a') AS tagline,
toUpper(p.name) AS upperName;

// Lists, brackets, MERGE actions, FOREACH, nested maps, and relationship patterns.
UNWIND [ {name: 'Alice', ROLES: ['lead', 'writer']}, {name: 'Bob', ROLES: ['support']}
] AS ROW
MERGE (person:Person {name: ROW.name})
  ON CREATE SET person.createdAt = datetime(), person.uuid = randomUUID()
  ON MATCH SET person.lastSeenAt = datetime()
FOREACH (ROLE IN ROW.ROLES |
MERGE (person)-[:HAS_ROLE]->(:ROLE {name: ROLE})
)
RETURN person;

// Subqueries, nested braces, folding ranges, and clause-based structure view entries.
CALL {
      MATCH (m: Movie)
      WHERE m.title CONTAINS $titlePart
      CALL {
          WITH m
          MATCH (m)<-[: ACTED_IN]-(actor: Person)
          RETURN collect(actor.name) AS actorNames
      }
      RETURN m.title AS title, actorNames
}
WITH title, actorNames
RETURN title, size(actorNames) AS actorCount, actorNames ORDER BY actorCount DESC, title ASC LIMIT 10;

// LET, path functions, list comprehensions, and normalization operators.
MATCH p = shortestPath(
(:Person {name: $fromName})-[:KNOWS * ..5]->(:Person {name: $toName})
)
LET summary = {hops: length(p),
names: [NODE IN nodes(p) | coalesce(NODE.name, 'unknown')]}
WHERE summary.names[0] IS NOT NULL
AND summary.names[0] IS NORMALIZED
RETURN p, summary;

// Administrative keywords from the extended lexer/completion set.
SHOW INDEXES YIELD name, type, state
RETURN name, type, state ORDER BY name ASC;

// ── UNION / UNION ALL ─────────────────────────────────────────────────────────
// Tests UNION, UNION ALL, DISTINCT, and multi-label nodes.
MATCH (n:Person:Director)
RETURN n.name AS name, 'director' AS ROLE
UNION ALL
MATCH (n:Person:Producer)
RETURN n.name AS name, 'producer' AS ROLE
UNION
MATCH (n:Person:Actor)
RETURN n.name AS name, 'actor' AS ROLE ORDER BY ROLE ASC, name ASC;

// ── USE + multi-database ───────────────────────────────────────────────────────
// Tests the USE clause and CALL IN TRANSACTIONS with error handling.
USE movies
MATCH (m:Movie)
WHERE m.released < 2000
CALL {
    WITH m
    DETACH
    DELETE m
} IN TRANSACTIONS OF 500 ROWS ON ERROR CONTINUE
REPORT STATUS AS batchStatus
RETURN batchStatus;

// ── EXISTS / COUNT / COLLECT subqueries ───────────────────────────────────────
// Tests inline subquery predicates introduced in Cypher 5.
MATCH (p:Person)
WHERE EXISTS {
    MATCH (p)-[:DIRECTED]->(m:Movie)
    WHERE m.released >= 2010
}
AND COUNT {
    MATCH (p)-[:ACTED_IN]->(:Movie)
} > 3
RETURN p.name AS name,
COLLECT {
    MATCH (p)-[:ACTED_IN]->(m:Movie)
    RETURN m.title ORDER BY m.released
} AS filmography;

// ── Quantified path patterns (QPP) ───────────────────────────────────────────
// Tests GQL-style quantified relationship patterns and named paths.
MATCH path = (
START:Person {name: $startName})
(()-[:KNOWS]->()) {1, 6}
(END:Person {name: $endName})
RETURN path, length(path) AS hops ORDER BY hops ASC LIMIT 3;

// ── SHORTEST with ANY GROUPS ──────────────────────────────────────────────────
// Tests SHORTEST, ANY, GROUPS, and REPEATABLE ELEMENTS keywords.
MATCH p = ANY SHORTEST
(:Person {name: $from})-[:KNOWS | WORKED_WITH] -+ (:Person {name: $to})
RETURN p;

// ── Relationship-type union patterns ─────────────────────────────────────────
// Tests multi-type relationship patterns (|) and variable-length ranges.
MATCH (a:Person)-[r:ACTED_IN | DIRECTED | PRODUCED * 1..3]->(target)
RETURN a.name AS person,
type(r[0]) AS firstRelType,
labels(target) AS targetLabels,
elementId(target) AS targetId;

// ── LOAD CSV ──────────────────────────────────────────────────────────────────
// Tests LOAD CSV WITH HEADERS, FIELDTERMINATOR, and toInteger/toFloat conversions.
LOAD CSV WITH HEADERS FROM $csvUrl AS ROW FIELDTERMINATOR ';'
WITH ROW
WHERE ROW.name IS NOT NULL
MERGE (p:Person {name: trim(ROW.name)})
SET p.born = toInteger(ROW.born),
p.rating = toFloat(ROW.rating),
p.ACTIVE = toBoolean(ROW.ACTIVE)
RETURN COUNT(p) AS imported;

// ── Temporal arithmetic and durations ────────────────────────────────────────
// Tests date(), datetime(), duration(), localdatetime(), and temporal operators.
WITH date('2024-01-01') AS
START,
datetime('2024-06-15T12:00:00') AS mid,
duration( {months: 6, days: 14}) AS d
RETURN
START + d AS endDate,
mid - duration( {hours: 2}) AS adjusted,
duration.between(
START, date()).days AS daysElapsed,
localdatetime( {year: 2025, month: 3, day: 1, hour: 9}) AS meeting;

// ── Spatial / point functions ─────────────────────────────────────────────────
// Tests point(), distance(), and withinBBox().
MATCH (venue:Venue)
WHERE withinBBox(venue.location,
POINT( {longitude: 18.0, latitude: 59.3}),
POINT( {longitude: 18.1, latitude: 59.4}))
WITH venue,
distance(venue.location, POINT( {longitude: 18.05, latitude: 59.33})) AS dist
RETURN venue.name AS venue,
round(dist) AS metersFromCenter ORDER BY dist ASC;

// ── reduce() and advanced list comprehensions ─────────────────────────────────
// Tests reduce(), list comprehensions with WHERE, and predicate functions.
MATCH p = (alice:Person {name: 'Alice'})-[:KNOWS * ..4]-(other:Person)
WITH nodes(p) AS hops
WHERE ALL(n IN hops
WHERE n.ACTIVE = TRUE)
AND none(n IN hops
WHERE n.blocked = TRUE)
AND ANY(n IN hops
WHERE n.premium = TRUE)
AND single(n IN hops
WHERE n.name ENDS
WITH 'admin')
RETURN reduce(acc = 0, n IN hops | acc + coalesce(n.score, 0)) AS totalScore,
[n IN hops
WHERE n.score > 10 | n.name] AS highScorers,
size(hops) AS pathLength;

// ── Math and string functions ─────────────────────────────────────────────────
// Tests a wide range of built-in scalar, math, trig, and string functions.
WITH 2.718281828 AS eulerApprox
RETURN abs(- 42) AS absVal,
ceil(3.2) AS ceiled,
floor(3.9) AS floored,
round(3.567, 2) AS rounded,
sqrt(144) AS sqrtVal,
log(eulerApprox) AS naturalLog,
log10(1000) AS log10Val,
sign(- 5) AS signNeg,
exp(1) AS expOne,
pi() AS piVal,
sin(radians(90)) AS sinVal,
cos(radians(0)) AS cosVal,
atan2(1, 1) AS atan2Val,
haversin(0.5) AS haversinVal,
toLower('HELLO') AS lower,
toUpper('world') AS upper,
trim('  spaced  ') AS trimmed,
lTrim('  left') AS leftTrimmed,
rTrim('right  ') AS rightTrimmed,
substring('abcdef', 2, 3) AS sub,
left('abcdef', 3) AS leftStr,
right('abcdef', 3) AS rightStr,
split('a,b,c', ',') AS parts,
REPLACE('foo bar', ' ', '_') AS replaced,
toString(42) AS strNum,
toStringOrNull(NULL) AS nullStr,
nullIf(42, 0) AS nullIfVal;

// ── Aggregation functions ─────────────────────────────────────────────────────
// Tests avg, sum, min, max, collect, stDev, stDevP, percentileCont, percentileDisc.
MATCH (m:Movie)<-[:ACTED_IN]-(p:Person)
WITH m.title AS title,
collect(DISTINCT p.name) AS cast,
COUNT( * ) AS castSize,
avg(p.born) AS avgBirth,
min(p.born) AS earliestBirth,
max(p.born) AS latestBirth,
sum(toInteger(p.born)) AS sumBirth,
stDev(toFloat(p.born)) AS stdev,
stDevP(toFloat(p.born)) AS stdevP
RETURN title, castSize, avgBirth, earliestBirth, latestBirth,
percentileCont(toFloat(stdev), 0.95) AS p95,
percentileDisc(toFloat(stdev), 0.5) AS median,
head(cast) AS firstActor,
last(cast) AS lastActor,
tail(cast) AS restOfCast,
reverse(cast) AS reversedCast ORDER BY castSize DESC LIMIT 20;

// ── Schema / index / constraint DDL ──────────────────────────────────────────
// Tests CREATE/DROP INDEX/CONSTRAINT, SHOW CONSTRAINTS, IF NOT EXISTS, OPTIONS.
CREATE INDEX person_name_range IF NOT EXISTS
FOR (p:Person) ON (p.name)
OPTIONS {indexConfig: {`spatial.cartesian.min`: [- 1000000, - 1000000]}};

CREATE FULLTEXT INDEX movieSearch IF NOT EXISTS
FOR (m:Movie) ON EACH [m.title, m.tagline];

CREATE VECTOR INDEX movieEmbeddings IF NOT EXISTS
FOR (m:Movie) ON (m.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}};

CREATE CONSTRAINT person_uuid_unique IF NOT EXISTS
FOR (p:Person) REQUIRE p.uuid IS UNIQUE;

CREATE CONSTRAINT movie_key IF NOT EXISTS
FOR (m:Movie) REQUIRE (m.title, m.released) IS NODE KEY;

SHOW CONSTRAINTS YIELD name, type, entityType, labelsOrTypes, properties, ownedIndex
RETURN name, type, entityType, labelsOrTypes, properties ORDER BY type ASC, name ASC;

DROP INDEX person_name_range IF EXISTS;

// ── User / role administration ────────────────────────────────────────────────
// Tests GRANT, DENY, REVOKE, CREATE USER/ROLE, and privilege keywords.
CREATE USER alice IF NOT EXISTS
SET PASSWORD $alicePassword CHANGE NOT REQUIRED
SET STATUS ACTIVE;

CREATE ROLE analyst IF NOT EXISTS;

GRANT ROLE analyst TO alice;

GRANT
MATCH {*} ON GRAPH movies TO analyst;
GRANT READ {title, released} ON GRAPH movies NODES Movie TO analyst;
DENY WRITE ON GRAPH movies TO analyst;

REVOKE
GRANT
MATCH {*} ON GRAPH movies FROM analyst;

SHOW PRIVILEGES AS COMMANDS YIELD command
RETURN command ORDER BY command ASC;

// ── EXPLAIN / PROFILE and YIELD ───────────────────────────────────────────────
// Tests EXPLAIN, PROFILE, and CALL … YIELD.
EXPLAIN
MATCH (n:Person)
WHERE n.born > 1970
RETURN n.name, n.born ORDER BY n.born DESC;

CALL db.schema.visualization() YIELD nodes, relationships
RETURN nodes, relationships;

CALL DBMS.listQueries() YIELD queryId, username, query, elapsedTimeMillis
WHERE elapsedTimeMillis > 5000
RETURN queryId, username, substring(query, 0, 80) AS querySnippet, elapsedTimeMillis ORDER BY elapsedTimeMillis DESC;

// ── ALTER / DROP (keywords now highlighted) ───────────────────────────────────
ALTER USER alice
SET PASSWORD $newPassword CHANGE NOT REQUIRED;

ALTER DATABASE movies
SET ACCESS READ ONLY WAIT;

DROP INDEX person_name_range IF EXISTS;

DROP CONSTRAINT person_uuid_unique IF EXISTS;

// ── TYPED property type constraints ──────────────────────────────────────────
CREATE CONSTRAINT person_name_typed IF NOT EXISTS
FOR (p:Person) REQUIRE p.name IS TYPED STRING;

CREATE CONSTRAINT edge_weight_typed IF NOT EXISTS
FOR ()-[r:KNOWS]-() REQUIRE r.weight IS TYPED FLOAT;

// ── Query hints ───────────────────────────────────────────────────────────────
// Tests USING INDEX SEEK, USING SCAN, USING JOIN ON.
MATCH (p:Person {name: $name})
USING INDEX SEEK p:Person(name)
RETURN p;

MATCH (p:Person)
USING SCAN p:Person
WHERE p.born > 1970
RETURN p.name;

MATCH (a:Person {name: $a}), (b:Person {name: $b})
USING JOIN ON a
MATCH (a)-[:KNOWS]->(b)
RETURN a, b;

// ── WAIT / NOWAIT / BRIEF / VERBOSE ──────────────────────────────────────────
START DATABASE movies WAIT 30;
STOP DATABASE movies NOWAIT;

SHOW CONSTRAINTS BRIEF;
SHOW INDEXES VERBOSE;

// ── DIFFERENT / SHORTEST variations ──────────────────────────────────────────
MATCH p = ALL SHORTEST
(:Person {name: $from})-[:KNOWS] -+ (:Person {name: $to})
WHERE ALL (n IN nodes(p)
WHERE n.ACTIVE)
RETURN p;

MATCH p = SHORTEST 3
(:Person {name: $from})-[:KNOWS] -* (:Person {name: $to})
RETURN DISTINCT p;

// ── Scientific notation numbers ───────────────────────────────────────────────
// Tests that 1.5e10, 2.3E-4, and 6.022E+23 lex as single NUMBER tokens.
WITH 1.5e10 AS big,
2.3E-4 AS small,
6.022E+23 AS avogadro,
0xFF AS hexVal,
1e6 AS million
RETURN big, small, avogadro, hexVal, million;

// ── Regex operator =~ and ~ ───────────────────────────────────────────────────
MATCH (p:Person)
WHERE p.name =~ '(?i)^alice.*'
RETURN p.name;

// ── Backtick-quoted identifiers ───────────────────────────────────────────────
MATCH (`my node`:Person)
WHERE `my node`.`first name` STARTS
WITH 'A'
RETURN `my node`.`first name` AS name;

// ── New list-conversion functions ─────────────────────────────────────────────
WITH ['1', '2', '3'] AS strs,
[1, 0, 1] AS ints
RETURN toIntegerList(strs) AS intList,
toFloatList(strs) AS floatList,
toBooleanList(ints) AS boolList,
toStringList(ints) AS strList;

// ── String functions: btrim, char ─────────────────────────────────────────────
RETURN btrim('  hello  ') AS trimmed,
char(65) AS letter;

// ── Vector similarity functions ───────────────────────────────────────────────
MATCH (m:Movie)
WHERE m.embedding IS NOT NULL
RETURN m.title,
VECTOR.similarity.cosine(m.embedding, $queryEmbedding) AS cosineSim,
VECTOR.similarity.euclidean(m.embedding, $queryEmbedding) AS euclidSim ORDER BY cosineSim DESC LIMIT 10;

// ── PERIODIC COMMIT (deprecated, still common in legacy scripts) ──────────────
USING PERIODIC COMMIT 1000
LOAD CSV FROM $csvUrl AS ROW
CREATE (:Person {name: ROW[0]});

// ── FINISH / NEXT (Cypher 5 GQL compatibility) ───────────────────────────────
// Tests NEXT (chain multiple query parts without WITH) and FINISH.
MATCH (p:Person {name: $name})
SET p.lastLogin = datetime()
NEXT
MATCH (p:Person {name: $name})-[:OWNS]->(d:Device)
SET d.lastSeen = datetime()
FINISH;

// ── String normalization (IS NORMALIZED / NFKC / NFC) ────────────────────────
// Tests IS NOT NORMALIZED, normalize(), and normalization-form keywords.
MATCH (u:USER)
WHERE u.username IS NOT NORMALIZED
AND NOT (u.username IS NFKC NORMALIZED)
SET u.username = normalize(u.username, NFC)
RETURN u.username AS fixed;
