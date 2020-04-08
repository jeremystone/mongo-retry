Performs multiple writes of small documents to mongo using the ReactiveMongo and Scala drivers in order to detect writes lost during replicaset re-elections.

Connects to a replicaset of three nodes: mongo1, mongo2 and mongo3. You will need to alias these to localhost in /etc/hosts.

A docker compose script is provided along with the initiate command to form the replicaset.

Run ReactiveMongoRetry or ScalaDriverMongoRetry and then execute rs.stepDown against the primary mongo node. 

On completion it will output the number of documents written to the database.