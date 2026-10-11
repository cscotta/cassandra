**Commit**: 2ee7e4d22c · **Ticket**: KAFKA-18152
**Subsystem**: tests/docker
**Category**: Logic error in condition

**Root cause**
Reviewers: Justine Olshan <jolshan@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
RUN curl -s "$KAFKA_MIRROR/kafka-streams-0.11.0.3-test.jar" -o /opt/kafka-0.11.0.3/libs/kafka-streams-0.11.0.3-test.jar
RUN curl -s "$KAFKA_MIRROR/kafka-streams-1.0.2-test.jar" -o /opt/kafka-1.0.2/libs/kafka-streams-1.0.2-test.jar
RUN curl -s "$KAFKA_MIRROR/kafka-streams-1.1.1-test.jar" -o /opt/kafka-1.1.1/libs/kafka-streams-1.1.1-test.jar
RUN curl -s "$KAFKA_MIRROR/kafka-streams-2.0.1-test.jar" -o /opt/kafka-2.0.1/libs/kafka-streams-2.0.1-test.jar
```

**How to spot**
When reviewing tests/docker code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18152 add 0.11, 1.0, 1.1, and 2.0 streams dependencies to dockerfile (#18025).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
