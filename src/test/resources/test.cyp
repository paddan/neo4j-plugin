LOAD CSV WITH HEADERS FROM 'https://data.neo4j.com/importing-cypher/movies.csv' AS row
CALL (row) {
    MERGE (m:Movie {
    movieId:row.movieId
    })
    MERGE (y:Year {
    year:row.year
    })
    MERGE (m)-[r:RELEASED_IN]->(y)
} IN 2 CONCURRENT TRANSACTIONS OF 10 ROWS
ON ERROR RETRY FOR 3 SECONDS THEN CONTINUE REPORT STATUS AS status
RETURN status.transactionId AS transaction, status.committed AS successfulTransaction
