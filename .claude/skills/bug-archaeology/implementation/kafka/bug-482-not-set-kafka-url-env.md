**Commit**: 68fbbbacb4 · **Ticket**: KAFKA-20093
**Subsystem**: docker/jvm
**Category**: Logic error in condition

**Root cause**
When creating a JVM Docker image for Kafka, due to recent(ish) changes
in the Dockerfile, we set KAFKA_URL as an environment variable. This
bakes the environment variable within the Docker image itself. In turn,
this causes a faulty server.properties to be generated, which only
contains a url=<URL> entry. This PR reverts to using an ARG.

**Before/After**
```java
// Before
ENV KAFKA_URL=$kafka_url

    if [ -n "$KAFKA_URL" ]; then \
        wget -nv -O kafka.tgz "$KAFKA_URL"; \
        wget -nv -O kafka.tgz.asc "$KAFKA_URL.asc"; \
ENV KAFKA_URL=$kafka_url

    if [ -n "$KAFKA_URL" ]; then \
        wget -nv -O kafka.tgz "$KAFKA_URL"; \
        wget -nv -O kafka.tgz.asc "$KAFKA_URL.asc"; \
ENV KAFKA_URL=$kafka_url
RUN if [ -n "$KAFKA_URL" ]; then \

// After
    if [ -n "$kafka_url" ]; then \
        wget -nv -O kafka.tgz "$kafka_url"; \
        wget -nv -O kafka.tgz.asc "$kafka_url.asc"; \
    if [ -n "$kafka_url" ]; then \
        wget -nv -O kafka.tgz "$kafka_url"; \
        wget -nv -O kafka.tgz.asc "$kafka_url.asc"; \
RUN if [ -n "$kafka_url" ]; then \
        wget -nv -O kafka.tgz "$kafka_url"; \
        wget -nv -O kafka.tgz.asc "$kafka_url.asc"; \
```

**How to spot**
When reviewing docker/jvm code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20093 do not set kafka_url as env in docker (#21362).

**Test**
No test added.
