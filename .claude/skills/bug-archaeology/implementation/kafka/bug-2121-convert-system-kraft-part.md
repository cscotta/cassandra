**Commit**: 58dd76817e · **Ticket**: KAFKA-17609
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
* Part 2 of 4 converting system tests to use KRaft

Reviewers: Matthias Sax <mjsax@apache.org>

**Before/After**
```java
// Before
from kafkatest.services.kafka import KafkaService
from kafkatest.services.zookeeper import ZookeeperService
    streams_eager_rebalance_upgrade_versions = [str(LATEST_0_10_0), str(LATEST_0_10_1), str(LATEST_0_10_2), str(LATEST_0_11_0),
        self.zookeeper = ZookeeperService(self.test_context, num_nodes=1)
                                  zk=self.zookeeper, topics=self.topics)
    @matrix(upgrade_from_version=streams_eager_rebalance_upgrade_versions)
    def test_upgrade_to_cooperative_rebalance(self, upgrade_from_version):
        self.zookeeper.start()
        self.zookeeper.stop()
    @matrix(metadata_quorum=[quorum.isolated_kraft])
    @matrix(metadata_quorum=[quorum.isolated_kraft])
    @matrix(metadata_quorum=[quorum.isolated_kraft])

// After
from kafkatest.services.kafka import KafkaService, quorum
    streams_eager_rebalance_upgrade_versions = [str(LATEST_0_10_2), str(LATEST_0_11_0),
                                  zk=None, topics=self.topics,
                                  controller_num_nodes_override=1)
    @matrix(upgrade_from_version=streams_eager_rebalance_upgrade_versions, metadata_quorum=[quorum.combined_kraft])
    def test_upgrade_to_cooperative_rebalance(self, upgrade_from_version, metadata_quorum):
    @matrix(metadata_quorum=[quorum.combined_kraft])
    @matrix(metadata_quorum=[quorum.combined_kraft])
    @matrix(metadata_quorum=[quorum.combined_kraft])
    @matrix(metadata_quorum=[quorum.combined_kraft])
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17609:[2/4]convert system tests to kraft part 2 (#17321).

**Test**
Test modified in streams_cooperative_rebalance_upgrade_test.py covering the corrected behaviour.
