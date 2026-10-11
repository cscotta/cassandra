**Commit**: 29881782c8 · **Ticket**: KAFKA-17609
**Subsystem**: Vagrantfile
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
jdk_major = '8'
jdk_full = '8u202-linux-x64'
from kafkatest.services.kafka import KafkaService
from kafkatest.services.zookeeper import ZookeeperService
from kafkatest.version import LATEST_1_0, LATEST_1_1, \
    LATEST_2_0, LATEST_2_1, LATEST_2_2, LATEST_2_3, LATEST_2_4, LATEST_2_5, LATEST_2_6, LATEST_2_7, LATEST_2_8, \
    LATEST_3_0, LATEST_3_1, LATEST_3_2, LATEST_3_3, LATEST_3_4, LATEST_3_5, LATEST_3_6, LATEST_3_7, LATEST_3_8, KafkaVersion
    - Streams works for older brokers 0.11 (or newer)
    - Streams w/ EOS-alpha works for older brokers 0.11 (or newer)
    - Streams fails fast for older brokers 0.10.0, 0.10.2, and 0.10.1
    - Streams w/ EOS-v2 fails fast for older brokers 2.4 or older
        self.zk = ZookeeperService(test_context, num_nodes=1)

// After
jdk_major = '11'
jdk_full = '11.0.2-linux-x64'
from kafkatest.services.kafka import KafkaService, quorum
from kafkatest.version import LATEST_3_0, LATEST_3_1, LATEST_3_2, LATEST_3_3, LATEST_3_4, LATEST_3_5, LATEST_3_6, LATEST_3_7, LATEST_3_8, KafkaVersion
                                  zk=None,
    @matrix(broker_version=[str(LATEST_3_0),str(LATEST_3_1),str(LATEST_3_2),str(LATEST_3_3),
                            str(LATEST_3_4),str(LATEST_3_5),str(LATEST_3_6),str(LATEST_3_7),
                            str(LATEST_3_8)],
            metadata_quorum=[quorum.combined_kraft]
            )
    def test_compatible_brokers_eos_disabled(self, broker_version, metadata_quorum):
    @matrix(broker_version=[str(LATEST_3_0),str(LATEST_3_1),str(LATEST_3_2),str(LATEST_3_3),
```

**How to spot**
When reviewing Vagrantfile code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17609 migrate broker compatibility test from zk to kraft (#17603).

**Test**
Test modified in streams_broker_compatibility_test.py covering the corrected behaviour.
