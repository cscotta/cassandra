**Commit**: 9026b6c6c0 · **Ticket**: KAFKA-18318
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
Add some logs when offline/online happens. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
        if (group == null || (createIfNotExists && maybeDeleteEmptyClassicGroup(group, records))) {
        maybeDeleteEmptyConsumerGroup(groupId, records);
     * @return true if the group is empty
    private void maybeDeleteEmptyConsumerGroup(String groupId, List<CoordinatorRecord> records) {

// After
        if (group == null) {
            return new ConsumerGroup(snapshotRegistry, groupId, metrics);
        } else if (createIfNotExists && maybeDeleteEmptyClassicGroup(group, records)) {
            log.info("[GroupId {}] Converted the empty classic group to a consumer group.", groupId);

        log.info("[GroupId {}] Converted the consumer group to a classic group.", consumerGroup.groupId());
        log.info("[GroupId {}] Converted the classic group to a consumer group.", classicGroup.groupId());

        if (maybeDeleteEmptyConsumerGroup(groupId, records)) {
            log.info("[GroupId {}] Converted the empty consumer group to a classic group.", groupId);
        }
     * @return true if the group is an empty classic group.
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18318: add logs for online/offline migration indication (#18406).

**Test**
No test added.
