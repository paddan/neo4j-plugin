MATCH (:Person { name:'Oliver Stone' })-->(movie:Movie)
RETURN movie.title AS movieTitle