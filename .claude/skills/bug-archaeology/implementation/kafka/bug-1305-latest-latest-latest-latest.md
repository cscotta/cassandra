**Commit**: 049430e9f9 · **Ticket**: KAFKA-18927
**Subsystem**: vagrant/base.sh
**Category**: Logic error in condition

**Root cause**
Revert the following commits:

https://github.com/apache/kafka/commit/2ee7e4d22c942045b960540918edc4960629a8a2

https://github.com/apache/kafka/commit/d1952e854202c574c1c19a48b17cc00e9d2eccd5

Reviewers: Matthias J. Sax <matthias@confluent.io>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
# The versions between 0.11.0.3 and 2.0.1 are used to run client code, because zookeeper in these versions is not compatible with JDK 17.
# See KAFKA-17888 for more details.
RUN mkdir -p "/opt/kafka-0.11.0.3" && chmod a+rw /opt/kafka-0.11.0.3 && curl -s "$KAFKA_MIRROR/kafka_2.11-0.11.0.3.tgz" | tar xz --strip-components=1 -C "/opt/kafka-0.11.0.3"
RUN mkdir -p "/opt/kafka-1.0.2" && chmod a+rw /opt/kafka-1.0.2 && curl -s "$KAFKA_MIRROR/kafka_2.11-1.0.2.tgz" | tar xz --strip-components=1 -C "/opt/kafka-1.0.2"
RUN mkdir -p "/opt/kafka-1.1.1" && chmod a+rw /opt/kafka-1.1.1 && curl -s "$KAFKA_MIRROR/kafka_2.11-1.1.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-1.1.1"
RUN mkdir -p "/opt/kafka-2.0.1" && chmod a+rw /opt/kafka-2.0.1 && curl -s "$KAFKA_MIRROR/kafka_2.12-2.0.1.tgz" | tar xz --strip-components=1 -C "/opt/kafka-2.0.1"
RUN curl -s "$KAFKA_MIRROR/kafka-streams-0.11.0.3-test.jar" -o /opt/kafka-0.11.0.3/libs/kafka-streams-0.11.0.3-test.jar
RUN curl -s "$KAFKA_MIRROR/kafka-streams-1.0.2-test.jar" -o /opt/kafka-1.0.2/libs/kafka-streams-1.0.2-test.jar
RUN curl -s "$KAFKA_MIRROR/kafka-streams-1.1.1-test.jar" -o /opt/kafka-1.1.1/libs/kafka-streams-1.1.1-test.jar
RUN curl -s "$KAFKA_MIRROR/kafka-streams-2.0.1-test.jar" -o /opt/kafka-2.0.1/libs/kafka-streams-2.0.1-test.jar
from kafkatest.version import (LATEST_0_11, LATEST_1_0, LATEST_1_1,
    LATEST_2_0, LATEST_2_1, LATEST_2_2, LATEST_2_3, LATEST_2_4, LATEST_2_5, LATEST_2_6, LATEST_2_7, LATEST_2_8, \

// After
from kafkatest.version import LATEST_2_1, LATEST_2_2, LATEST_2_3, LATEST_2_4, LATEST_2_5, LATEST_2_6, LATEST_2_7, LATEST_2_8, \
    DEV_BRANCH, DEV_VERSION, KafkaVersion
        self.set_up_services()
    def set_up_services(self):
        self.kafka = KafkaService(self.test_context, num_nodes=1, zk=None, topics=self.topics)
    assert len(lines) == 1, "lines: %s" % l
```

**How to spot**
When reviewing vagrant/base.sh code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18927 remove latest_0_11, latest_1_0, latest_1_1, latest_2_0 (#19134).

**Test**
Test modified in Dockerfile covering the corrected behaviour.
