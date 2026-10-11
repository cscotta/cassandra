**Commit**: 7436159a27 · **Ticket**: KAFKA-18449
**Subsystem**: config/kraft
**Category**: Logic error in condition

**Root cause**
Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
# The replication factor for the group metadata internal topics "__consumer_offsets" and "__transaction_state"
# The replication factor for the group metadata internal topics "__consumer_offsets" and "__transaction_state"
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1

# Share state topic settings

// After
# The replication factor for the group metadata internal topics "__consumer_offsets", "__share_group_state" and "__transaction_state"
share.coordinator.state.topic.replication.factor=1
share.coordinator.state.topic.min.isr=1
# The replication factor for the group metadata internal topics "__consumer_offsets", "__share_group_state" and "__transaction_state"
transaction.state.log.replication.factor=1
transaction.state.log.min.isr=1
```

**How to spot**
When reviewing config/kraft code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18449: add share group state configs to reconfig-server.properties (#18440).

**Test**
No test added.
