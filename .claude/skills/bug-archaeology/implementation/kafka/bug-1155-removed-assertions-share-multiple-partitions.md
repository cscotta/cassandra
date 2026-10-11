**Commit**: b088ba7999 · **Ticket**: KAFKA-19181
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
The system test `ShareConsumerTest.test_share_multiple_partitions`
started failing because of the recent change in the SimpleAssignor
algorithm. The tests assumed that if a share group is subscribed to a
topic, then every share consumers part of the group will be assigned all
partitions of the topic. But that does not happen now, and partitions
are split between the share consumers in certain cases, in which some
partitions are only assigned to a subset of share consumers. This change
removes that assumption

Reviewers: PoAn Yang <payang@apache.org>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            for topic_partition in self.get_topic_partitions(self.TOPIC2):
                assert topic_partition in event_handler.consumed_per_partition
                assert event_handler.consumed_per_partition[topic_partition] > 0
                assert topic_partition in event_handler.acknowledged_per_partition
                assert event_handler.acknowledged_per_partition[topic_partition] > 0

// After
// (no additions)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19181: removed assertions in test_share_multiple_partitions as a result of change in assignor algorithm (#19541).

**Test**
Test modified in share_consumer_test.py covering the corrected behaviour.
