**Commit**: a599b89fe0 · **Ticket**: KAFKA-16275
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This reverts commit e95e91a. With the change to include the group.version flag, these tests fail due to trying to set the feature for the old version. It is unclear if these tests originally worked as intended and given the upgrade is not expected for 3.8, we will just revert from 3.8. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
from kafkatest.services.kafka import KafkaService, consumer_group
from kafkatest.version import LATEST_3_1, LATEST_3_2, LATEST_3_3, LATEST_3_4, LATEST_3_5, LATEST_3_7, \
    DEV_BRANCH, KafkaVersion, LATEST_STABLE_METADATA_VERSION
    def run_upgrade(self, from_kafka_version, group_protocol):
                                        message_validator=is_int, version=KafkaVersion(from_kafka_version),
                                        consumer_properties=consumer_group.maybe_set_group_protocol(group_protocol))
    @matrix(from_kafka_version=[str(LATEST_3_1), str(LATEST_3_2), str(LATEST_3_3), str(LATEST_3_4), str(LATEST_3_5), str(DEV_BRANCH)],
            use_new_coordinator=[True, False],
    @matrix(from_kafka_version=[str(LATEST_3_7), str(DEV_BRANCH)],
            use_new_coordinator=[True],
            metadata_quorum=[combined_kraft],
            group_protocol=consumer_group.all_group_protocols)

// After
from kafkatest.services.kafka import KafkaService
from kafkatest.version import LATEST_3_1, LATEST_3_2, LATEST_3_3, LATEST_3_4, LATEST_3_5, \
    LATEST_3_6, LATEST_3_7, DEV_BRANCH, KafkaVersion, LATEST_STABLE_METADATA_VERSION
    def run_upgrade(self, from_kafka_version):
                                        message_validator=is_int, version=KafkaVersion(from_kafka_version))
    @matrix(from_kafka_version=[str(LATEST_3_1), str(LATEST_3_2), str(LATEST_3_3), str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(DEV_BRANCH)],
    def test_combined_mode_upgrade(self, from_kafka_version, metadata_quorum, use_new_coordinator=False):
        self.run_upgrade(from_kafka_version)
    @matrix(from_kafka_version=[str(LATEST_3_1), str(LATEST_3_2), str(LATEST_3_3), str(LATEST_3_4), str(LATEST_3_5), str(LATEST_3_6), str(LATEST_3_7), str(DEV_BRANCH)],
    def test_isolated_mode_upgrade(self, from_kafka_version, metadata_quorum, use_new_coordinator=False):
        self.run_upgrade(from_kafka_version)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: revert "kafka-16275: update kraft_upgrade_test.py to support kip-848’s group protocol config (#16409) (#16441).

**Test**
Test modified in kraft_upgrade_test.py covering the corrected behaviour.
