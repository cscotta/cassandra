**Commit**: 6147a311bf · **Ticket**: KAFKA-17888
**Subsystem**: tests/docker
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
# The maximum mmemory consumption to allow in containers.

// After
# To ensure the Kafka cluster starts successfully under JDK 17, we need to update the Zookeeper
# client from version 3.4.x to 3.5.7 in Kafka versions 2.1.1, 2.2.2, and 2.3.1, as the older Zookeeper
# client is incompatible with JDK 17. See KAFKA-17888 for more details.
RUN curl -s "https://repo1.maven.org/maven2/org/apache/zookeeper/zookeeper/3.5.7/zookeeper-3.5.7.jar" -o /opt/zookeeper-3.5.7.jar
RUN curl -s "https://repo1.maven.org/maven2/org/apache/zookeeper/zookeeper-jute/3.5.7/zookeeper-jute-3.5.7.jar" -o /opt/zookeeper-jute-3.5.7.jar
RUN rm -f /opt/kafka-2.1.1/libs/zookeeper-* && rm -f /opt/kafka-2.2.2/libs/zookeeper-* && rm -f /opt/kafka-2.3.1/libs/zookeeper-*
RUN cp /opt/zookeeper-3.5.7.jar /opt/kafka-2.1.1/libs/zookeeper-3.5.7.jar && chmod a+rw /opt/kafka-2.1.1/libs/zookeeper-3.5.7.jar
RUN cp /opt/zookeeper-3.5.7.jar /opt/kafka-2.2.2/libs/zookeeper-3.5.7.jar && chmod a+rw /opt/kafka-2.2.2/libs/zookeeper-3.5.7.jar
RUN cp /opt/zookeeper-3.5.7.jar /opt/kafka-2.3.1/libs/zookeeper-3.5.7.jar && chmod a+rw /opt/kafka-2.3.1/libs/zookeeper-3.5.7.jar
RUN cp /opt/zookeeper-jute-3.5.7.jar /opt/kafka-2.1.1/libs/zookeeper-jute-3.5.7.jar && chmod a+rw /opt/kafka-2.1.1/libs/zookeeper-jute-3.5.7.jar
RUN cp /opt/zookeeper-jute-3.5.7.jar /opt/kafka-2.2.2/libs/zookeeper-jute-3.5.7.jar && chmod a+rw /opt/kafka-2.2.2/libs/zookeeper-jute-3.5.7.jar
RUN cp /opt/zookeeper-jute-3.5.7.jar /opt/kafka-2.3.1/libs/zookeeper-jute-3.5.7.jar && chmod a+rw /opt/kafka-2.3.1/libs/zookeeper-jute-3.5.7.jar
```

**How to spot**
When reviewing tests/docker code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17888 upgrade zookeeper version from 3.4.9 to 3.5.7 to avoid zookeeper-3779, which can't run under jdk 11. (#17625.

**Test**
Test modified in Dockerfile covering the corrected behaviour.
