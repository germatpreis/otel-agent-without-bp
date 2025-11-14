# Builder: builds maven multi-module workspace
FROM maven:3-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY . .
RUN pwd
RUN --mount=type=cache,target=/root/.m2 ./mvnw package -DskipTests
RUN #ls -l foobar-service-layered-jar/target

RUN java -Djarmode=layertools -jar foobar-service-layered-jar/target/foobar-service-layered-jar-0.0.1-SNAPSHOT.jar extract

RUN ls -l

# Build Service Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copies layers from spring-boot service
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/application/ ./

# Copies the customized agent (offical agent + my extension bundled into one jar)
COPY --from=builder /build/agents/opentelemetry/opentelemetry-javaagent-foobar/target/opentelemetry-javaagent-foobar-2.19.0.jar agents/opentelemetry-javaagent-foobar-2.19.0.jar

# Sets JAVA_TOOL_OPTIONS to pick up the agent
ENV JAVA_TOOL_OPTIONS=-javaagent:/app/agents/opentelemetry-javaagent-foobar-2.19.0.jar

# Configures OTEL
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_METRIC_EXPORT_INTERVAL=500
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_SERVICE_NAME=foobar-service
ENV OTEL_TRACES_EXPORTER=otlp
#ENV OTEL_JAVAAGENT_LOGGING=application

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]