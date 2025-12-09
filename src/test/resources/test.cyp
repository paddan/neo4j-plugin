MATCH (:Person {name:'Andy'})-[k:KNOWS]->(f)
WHERE k.since < 2000
RETURN f.name AS oldFriend