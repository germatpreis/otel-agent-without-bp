# Builder: builds maven multi-module workspace
FROM maven:3-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY . .
RUN ./mvnw dependency:go-offline
RUN ./mvnw package -DskipTests

# Build Service Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copies spring-boot service
COPY --from=builder /build/foobar-service/target/foobar-service-0.0.1-SNAPSHOT.jar /app/foobar-service-0.0.1-SNAPSHOT.jar
# Copies the customized agent (offical agent + my extension bundled into one jar)
COPY --from=builder /build/agents/opentelemetry/opentelemetry-javaagent-foobar/target/opentelemetry-javaagent-foobar-2.6.0.jar /app/opentelemetry-javaagent-foobar-2.6.0.jar
# Sets JAVA_TOOL_OPTIONS to pick up the agent
ENV JAVA_TOOL_OPTIONS=-javaagent:opentelemetry-javaagent-foobar-2.6.0.jar
# Configures OTEL
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_METRIC_EXPORT_INTERVAL=500
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_SERVICE_NAME=foobar-service
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_JAVAAGENT_LOGGING=application

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/foobar-service-0.0.1-SNAPSHOT.jar"]