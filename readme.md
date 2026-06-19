# Run

1. `mvn install -DskipTests`
2. `docker-compose -f docker-compose.yml -f docker-compose-kafka.yml up -d`
3. `mvn schema-registry:register -pl api/avro-api`

# Ports

## Services

| Service | Port |
|---|---|
| `package-service` | 8080 |
| `audittrail-service` | 8082 |
| `deployment-service` | 8083 |
| `policy-engine` | 8084 |

## Infrastructure (`docker-compose.yml`)

| Service | Port |
|---|---|
| Postgres | 5432 |
| Temporal | 7233 |
| [Temporal UI](http://localhost:8089) | 8089 |
| Grafana / OTel LGTM | 51423 |
| OTel Collector gRPC | 4317 |
| OTel Collector HTTP | 4318 |

## Kafka (`docker-compose-kafka.yml`)

| Service | Port |
|---|---|
| Kafka broker | 9092 |
| Kafka JMX | 9101 |
| Schema Registry | 8081 |
| [Redpanda Console](http://localhost:9005) | 9005 |
