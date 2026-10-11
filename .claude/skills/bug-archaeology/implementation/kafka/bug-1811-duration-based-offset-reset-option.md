**Commit**: 220c578521 · **Ticket**: KAFKA-18014
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Kafka consumer supports auto.offset.reset config option, which is used when there is no initial offset in Kafka (or) if the current offset does not exist any more on the server. This config currently supports earliest/latest/none options. Currently consumer resets might force applications to reprocess large amounts of data from earlier offsets. With infinite storage, its beneficial to have a duration based offset reset strategy.

**Before/After**
```java
// Before
        GroupConfig.ShareGroupAutoOffsetReset offsetResetStrategy;
        if (offsetResetStrategy == GroupConfig.ShareGroupAutoOffsetReset.EARLIEST)
        return offsetForLatestTimestamp(topicIdPartition, replicaManager, leaderEpoch);

// After
    /**
     * The method is used to get the offset for the given timestamp for the topic-partition.
     *
     * @return The offset for the given timestamp.
     */
    static long offsetForTimestamp(TopicIdPartition topicIdPartition, ReplicaManager replicaManager, long timestampToSearch, int leaderEpoch) {
        Option<FileRecords.TimestampAndOffset> timestampAndOffset = replicaManager.fetchOffsetForTimestamp(
            topicIdPartition.topicPartition(), timestampToSearch, new Some<>(IsolationLevel.READ_UNCOMMITTED), Optional.of(leaderEpoch), true).timestampAndOffsetOpt();
        if (timestampAndOffset.isEmpty()) {
            throw new OffsetNotAvailableException("Offset for timestamp " + timestampToSearch + " not found for topic partition: " + topicIdPartition);
        }
        return timestampAndOffset.get().offset;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18014: add duration based offset reset option for shareconsumer (#18096).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
