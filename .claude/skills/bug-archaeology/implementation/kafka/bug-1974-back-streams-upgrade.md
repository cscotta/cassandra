**Commit**: d1952e8542 · **Ticket**: KAFKA-18045
**Subsystem**: vagrant/base.sh
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
from kafkatest.version import LATEST_2_1, LATEST_2_2, LATEST_2_3, LATEST_2_4, LATEST_2_5, LATEST_2_6, LATEST_2_7, LATEST_2_8, \
    DEV_BRANCH, DEV_VERSION, KafkaVersion
metadata_2_versions = [str(LATEST_2_4), str(LATEST_2_5), str(LATEST_2_6), str(LATEST_2_7), str(LATEST_2_8),
        self.set_up_services()
    def set_up_services(self):
        self.kafka = KafkaService(self.test_context, num_nodes=1, zk=None, topics=self.topics)

// After
# The versions between 0.11.0.3 and 2.0.1 are used to run client code, because zookeeper in these versions is not compatible with JDK 17.
# See KAFKA-17888 for more details.
RUN mkdir -p "/opt/kafka-0.11.0.3" && chmod a+rw /opt/kafka-0.11.0.3 && curl -s "$KAFKA_MIRROR/kafka_2.11-0.11.0.3.tgz" | tar xz --strip-components=1 -C "/opt/kafka-0.11.0.3"
RUN mkdir -p "/opt/kafka-1.0.2" && chmod a+rw /opt/kafka-1.0.2 && curl -s "$KAFKA_MIRROR/kafka_2.11-1.0.2.tgz" | tar xz --strip-components=1 -C "/opt/kafka-1.0.2"
RUN mkdir -p "/opt/kafka-1.1.1" && chmod a+rw /opt/kafka-1.1.1 && curl -s "$KAFKA_MIRROR/kafka_2.11-1.1.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-1.1.1"
RUN mkdir -p "/opt/kafka-2.0.1" && chmod a+rw /opt/kafka-2.0.1 && curl -s "$KAFKA_MIRROR/kafka_2.12-2.0.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-2.0.1"
from kafkatest.version import (LATEST_0_11, LATEST_1_0, LATEST_1_1,
    LATEST_2_0, LATEST_2_1, LATEST_2_2, LATEST_2_3, LATEST_2_4, LATEST_2_5, LATEST_2_6, LATEST_2_7, LATEST_2_8, \
    DEV_BRANCH, DEV_VERSION, KafkaVersion)
metadata_2_versions = [str(LATEST_0_11), str(LATEST_1_0), str(LATEST_1_1), str(LATEST_2_0),
                       str(LATEST_2_4), str(LATEST_2_5), str(LATEST_2_6), str(LATEST_2_7), str(LATEST_2_8),
        broker_version = DEV_BRANCH
```

**How to spot**
When reviewing vagrant/base.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18045 add 0.11, 1.0, 1.1, and 2.0 back to streams_upgrade_test.py (#17876).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
