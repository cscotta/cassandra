**Commit**: aa4782aea3 · **Ticket**: KAFKA-17576
**Subsystem**: README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    ./bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
    ./bin/kafka-server-start.sh config/kraft/server.properties
    cp /opt/kafka/config/kraft/server.properties /etc/kafka/docker/server.properties; \
KAFKA_JVM_PERFORMANCE_OPTS="-XX:ArchiveClassesAtExit=storage.jsa" opt/kafka/bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c opt/kafka/config/kraft/server.properties
KAFKA_JVM_PERFORMANCE_OPTS="-XX:ArchiveClassesAtExit=kafka.jsa" opt/kafka/bin/kafka-server-start.sh opt/kafka/config/kraft/server.properties &
    cp /opt/kafka/config/kraft/server.properties /etc/kafka/docker/server.properties; \
KAFKA_JVM_PERFORMANCE_OPTS="-XX:ArchiveClassesAtExit=storage.jsa" opt/kafka/bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c opt/kafka/config/kraft/server.properties
KAFKA_JVM_PERFORMANCE_OPTS="-XX:ArchiveClassesAtExit=kafka.jsa" opt/kafka/bin/kafka-server-start.sh opt/kafka/config/kraft/server.properties &
COPY --chown=appuser:root --from=build-native-image /app/kafka/config/kraft/server.properties /etc/kafka/docker/
<pre><code class="language-bash">$ bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties</code></pre>
<pre><code class="la

// After
    ./bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/reconfig-server.properties
    ./bin/kafka-server-start.sh config/kraft/reconfig-server.properties
    cp /opt/kafka/config/kraft/reconfig-server.properties /etc/kafka/docker/server.properties; \
KAFKA_JVM_PERFORMANCE_OPTS="-XX:ArchiveClassesAtExit=storage.jsa" opt/kafka/bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c opt/kafka/config/kraft/reconfig-server.properties
KAFKA_JVM_PERFORMANCE_OPTS="-XX:ArchiveClassesAtExit=kafka.jsa" opt/kafka/bin/kafka-server-start.sh opt/kafka/config/kraft/reconfig-server.properties &
    cp /opt/kafka/config/kraft/reconfig-server.properties /etc/kafka/docker/server.properties; \
KAFKA_JVM_PERFORMANCE_OPTS="-XX:ArchiveClassesAtExit=storage.jsa" opt/kafka/bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c opt/kafka/config/kraft/reconfig-server.properties
KAFKA_JVM_PERFORMANCE_OPTS="-XX:ArchiveClassesAtExit=kafka.jsa" opt/kafka/bin/kafka-server-start.sh opt/kafka/config/kraft/reconfig-server.properties &
COPY --chown=appuser:root --from=build-native-image /app/kafka/config/kraft/reconfig-server.properties /etc/kafka/docker/
<pre><code class="language-bash">$ bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/reconfig-server.properties</code></pre>
```

**How to spot**
When reviewing README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17576 fix all references to kraft/server.properties to use reconfig-server.properties (#17567).

**Test**
No test added.
