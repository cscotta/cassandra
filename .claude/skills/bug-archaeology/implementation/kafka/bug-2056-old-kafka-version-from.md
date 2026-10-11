**Commit**: 440e0b8801 · **Ticket**: KAFKA-17923
**Subsystem**: vagrant/base.sh
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
RUN mkdir -p "/opt/kafka-0.8.2.2" && chmod a+rw /opt/kafka-0.8.2.2 && curl -s "$KAFKA_MIRROR/kafka_2.11-0.8.2.2.tgz" | tar xz --strip-components=1 -C "/opt/kafka-0.8.2.2"
RUN mkdir -p "/opt/kafka-0.9.0.1" && chmod a+rw /opt/kafka-0.9.0.1 && curl -s "$KAFKA_MIRROR/kafka_2.11-0.9.0.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-0.9.0.1"
RUN mkdir -p "/opt/kafka-0.10.0.1" && chmod a+rw /opt/kafka-0.10.0.1 && curl -s "$KAFKA_MIRROR/kafka_2.11-0.10.0.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-0.10.0.1"
RUN mkdir -p "/opt/kafka-0.10.1.1" && chmod a+rw /opt/kafka-0.10.1.1 && curl -s "$KAFKA_MIRROR/kafka_2.11-0.10.1.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-0.10.1.1"
RUN mkdir -p "/opt/kafka-0.10.2.2" && chmod a+rw /opt/kafka-0.10.2.2 && curl -s "$KAFKA_MIRROR/kafka_2.11-0.10.2.2.tgz" | tar xz --strip-components=1 -C "/opt/kafka-0.10.2.2"
RUN mkdir -p "/opt/kafka-0.11.0.3" && chmod a+rw /opt/kafka-0.11.0.3 && curl -s "$KAFKA_MIRROR/kafka_2.11-0.11.0.3.tgz" | tar xz --strip-components=1 -C "/opt/kafka-0.11.0.3"
RUN mkdir -p "/opt/kafka-1.0.2" && chmod a+rw /opt/kafka-1.0.2 && curl -s "$KAFKA_MIRROR/kafka_2.11-1.0.2.tgz" | tar xz --strip-components=1 -C "/opt/kafka-1.0.2"
RUN mkdir -p "/opt/kafka-1.1.1" && chmod a+rw /opt/kafka-1.1.1 && curl -s "$KAFKA_MIRROR/kafka_2.11-1.1.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-1.1.1"
RUN mkdir -p "/opt/kafka-2.0.1" && chmod a+rw /opt/kafka-2.0.1 && curl -s "$KAFKA_MIRROR/kafka_2.12-2.0.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-2.0.1"
RUN curl -s "$KAFKA_MIRROR/kafka-streams-0.10.0.1-test.jar" -o /opt/kafka-0.10.0.1/libs/kafka-streams-0.10.0.1-test.jar
RUN curl -s "$KAFKA_MIRROR/kafka-streams-0.10.1.1-test.jar" -o /opt/kafka-0.10.1.1/libs/kafka-streams-0.10.1.1-test.jar
RUN curl -s "$KAFKA_MIRROR/kafka-streams-0.10.2.2-test.jar" -o /opt/kafka-0.10.2.2/libs/kafka-streams-0.10.2.2-test.jar

// After
from kafkatest.version import get_version, KafkaVersion, DEV_BRANCH, LATEST_3_5
```

**How to spot**
When reviewing vagrant/base.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17923 remove old kafka version from e2e (#17673).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
