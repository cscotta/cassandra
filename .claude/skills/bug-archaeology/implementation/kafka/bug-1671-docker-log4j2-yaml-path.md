## Commit

- Hash: cd061c8039
- Tickets: KAFKA-18340
- Subject: KAFKA-18340: Update Docker images to reference log4j2.yaml instead of log4j.properties

## Subsystem

docker / container image configuration

## Category

Wrong constant / default value

## Root cause

The Docker `jvm/Dockerfile` and `native/Dockerfile` were still referencing the legacy `log4j.properties` and `tools-log4j.properties` file paths via environment variables (`KAFKA_LOG4J_OPTS`). After the migration to Log4j2 YAML configuration, the correct files are `log4j2.yaml` and `tools-log4j2.yaml`. Containers started with the old paths would fall back to Log4j2 default configuration (root logger at WARN), silently suppressing most broker logs.

## Before

```dockerfile
ENV KAFKA_LOG4J_OPTS="-Dlog4j.configuration=file:/opt/kafka/config/log4j.properties"
```

## After

```dockerfile
ENV KAFKA_LOG4J_OPTS="-Dlog4j2.configurationFile=/opt/kafka/config/log4j2.yaml"
```

## How to spot

Docker ENV or CMD entries that reference `log4j.properties` or `log4j.configuration` (Log4j 1.x / Log4j2 compatibility mode) when the actual deployed config files have been migrated to `log4j2.yaml`.

## Test

Docker image build smoke test verifies that broker logs appear at the expected levels.
