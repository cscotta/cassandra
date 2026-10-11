**Commit**: 8c55dcc979 · **Ticket**: KAFKA-18237
**Subsystem**: gradle/dependencies.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
  kafka_37: "3.7.1",
RUN mkdir -p "/opt/kafka-3.7.1" && chmod a+rw /opt/kafka-3.7.1 && curl -s "$KAFKA_MIRROR/kafka_2.12-3.7.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-3.7.1"
RUN curl -s "$KAFKA_MIRROR/kafka-streams-3.7.1-test.jar" -o /opt/kafka-3.7.1/libs/kafka-streams-3.7.1-test.jar
LATEST_3_7 = V_3_7_1
get_kafka 3.7.1 2.12
chmod a+rw /opt/kafka-3.7.1

// After
  kafka_37: "3.7.2",
RUN mkdir -p "/opt/kafka-3.7.2" && chmod a+rw /opt/kafka-3.7.2 && curl -s "$KAFKA_MIRROR/kafka_2.12-3.7.2.tgz" | tar xz --strip-components=1 -C "/opt/kafka-3.7.2"
RUN curl -s "$KAFKA_MIRROR/kafka-streams-3.7.2-test.jar" -o /opt/kafka-3.7.2/libs/kafka-streams-3.7.2-test.jar
V_3_7_2 = KafkaVersion("3.7.2")
LATEST_3_7 = V_3_7_2
get_kafka 3.7.2 2.12
chmod a+rw /opt/kafka-3.7.2
```

**How to spot**
When reviewing gradle/dependencies.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18237: upgrade system tests from using 3.7.1 to 3.7.2 (#18180).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
