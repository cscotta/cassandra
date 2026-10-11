**Commit**: 3d2edf8de0 · **Ticket**: KAFKA-17609
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Part 4 of 4 converting streams system tests to KRaft

Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
            metadata_quorum=quorum.all_non_upgrade)
    def test_streams(self, processing_guarantee, crash, metadata_quorum=quorum.zk):
from kafkatest.services.zookeeper import ZookeeperService
        self.zookeeper = (
            ZookeeperService(self.test_context, 1)
            if quorum.for_test(self.test_context) == quorum.zk
            else None
        )
                                  zk=self.zookeeper, topics=self.topics, controller_num_nodes_override=1)
        if self.zookeeper:
            self.zookeeper.start()
        if self.zookeeper:

// After
            metadata_quorum=[quorum.combined_kraft])
    def test_streams(self, processing_guarantee, crash, metadata_quorum):
                                  zk=None, topics=self.topics, controller_num_nodes_override=1)
from kafkatest.services.kafka import KafkaService, quorum
from kafkatest.version import LATEST_0_10_2, LATEST_0_11_0, LATEST_1_0, LATEST_1_1, \
broker_upgrade_versions = [str(LATEST_2_8), str(LATEST_3_0), str(LATEST_3_1), str(LATEST_3_2),
metadata_2_versions = [str(LATEST_0_10_2), str(LATEST_0_11_0), str(LATEST_1_0), str(LATEST_1_1),
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17609:[4/4]convert system tests to kraft part 4 (#17328).

**Test**
Test modified in streams_smoke_test.py covering the corrected behaviour.
