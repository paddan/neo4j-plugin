MATCH (u:User { id:$userId, name:$(userName) })-[:FRIEND]->(friend) WHERE friend.active = true
CALL {
    WITH $userId
    RETURN COUNT( * ) AS c
}
RETURN DISTINCT friend.name, friend.age, c ORDER BY friend.age DESC LIMIT 10;