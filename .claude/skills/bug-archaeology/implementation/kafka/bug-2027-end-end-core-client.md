**Commit**: e9cd9c9811 · **Ticket**: KAFKA-18006
**Subsystem**: vagrant/base.sh
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
from kafkatest.version import LATEST_3_8
    # After 4.0, zookeeper service is removed from source code. Using LATEST_3_8 for compatibility test cases.
                 zk_tls_encrypt_only = False, version=LATEST_3_8):
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_8)

// After
RUN mkdir -p "/opt/kafka-3.9.0" && chmod a+rw /opt/kafka-3.9.0 && curl -s "$KAFKA_MIRROR/kafka_2.12-3.9.0.tgz" | tar xz --strip-components=1 -C "/opt/kafka-3.9.0"
RUN curl -s "$KAFKA_MIRROR/kafka-streams-3.9.0-test.jar" -o /opt/kafka-3.9.0/libs/kafka-streams-3.9.0-test.jar
from kafkatest.version import LATEST_3_9
    # After 4.0, zookeeper service is removed from source code. Using LATEST_3_9 for compatibility test cases.
                 zk_tls_encrypt_only = False, version=LATEST_3_9):
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_9)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_9)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_9)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_9)
        kafka_run_class = self.path.script("kafka-run-class.sh", LATEST_3_9)
```

**How to spot**
When reviewing vagrant/base.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18006 add 3.9.0 to end-to-end test (core, client) (#17797).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
