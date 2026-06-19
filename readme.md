# Run

1. `mvn install -DskipTests`
2. `docker-compose -f docker-compose.yml -f docker-compose-kafka.yml up -d`
3. `mvn schema-registry:register -pl api/avro-api`

