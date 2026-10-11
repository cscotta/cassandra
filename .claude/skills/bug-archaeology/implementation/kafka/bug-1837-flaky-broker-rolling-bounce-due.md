**Commit**: ce77a7413e · **Ticket**: KAFKA-18194
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                 on_record_consumed=None, reset_policy="earliest", verify_offsets=True):
        self.prop_file = ""
        partition, an a set of consumers in the same group reading from the same topic.
        consumer = self.setup_consumer(self.TOPIC, group_protocol=group_protocol)

// After
                 on_record_consumed=None, reset_policy="earliest", verify_offsets=True, prop_file=""):
        self.prop_file = prop_file
        partition, a set of consumers in the same group reading from the same topic.
        # Due to KIP-899, which rebootstrap is performed when there are no available brokers in the current metadata.
        # We disable rebootstrapping by setting `metadata.recovery.strategy=none` for the consumer, as the test expects no metadata changes.
        # see KAFKA-18194
        consumer = self.setup_consumer(self.TOPIC, group_protocol=group_protocol, prop_file="metadata.recovery.strategy=none")
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18194 flaky test_broker_rolling_bounce due to metadata update (#18153).

**Test**
Test modified in verifiable_consumer.py covering the corrected behaviour.
