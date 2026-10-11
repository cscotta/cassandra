**Commit**: 358d8775fb · **Ticket**: KAFKA-17609
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Part 3 of 4 converting streams system tests to KRaft

Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
from kafkatest.services.zookeeper import ZookeeperService
        self.zookeeper = (
            ZookeeperService(self.test_context, 1)
            if quorum.for_test(self.test_context) == quorum.zk
            else None
        )
        self.kafka = KafkaService(self.test_context, num_nodes=3,
                                  zk=self.zookeeper, topics=self.topics, controller_num_nodes_override=1)
    @matrix(metadata_quorum=[quorum.isolated_kraft])
        if self.zookeeper:
            self.zookeeper.start()
        if self.zookeeper:

// After
        self.kafka = KafkaService(self.test_context, num_nodes=3, controller_num_nodes_override=1,
                                  zk=None, topics=self.topics)
    @matrix(metadata_quorum=[quorum.combined_kraft])
            metadata_quorum=[quorum.combined_kraft])
    def test_streams(self, crash, metadata_quorum):
        processing_guarantee='exactly_once_v2'
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17609:[3/4]convert system tests to kraft part 3 (#17327).

**Test**
Test modified in streams_optimized_test.py covering the corrected behaviour.
