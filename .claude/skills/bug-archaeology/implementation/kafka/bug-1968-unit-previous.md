**Commit**: 95947d2f58 · **Ticket**: KAFKA-17299
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
https://github.com/apache/kafka/pull/17899 fixed the issue, but did not
add any unit tests. Reviewers: Bill Bejeck <bill@confluent.io>

**Before/After**
```java
// Before
                --totalBuffered;
    private final TopicPartition partition1 = new TopicPartition(topic1, 1);
    private final TopicPartition partition2 = new TopicPartition(topic2, 1);

// After
            final int oldSize = queue.size();
                totalBuffered -= oldSize - queue.size();
    @Test
    public void shouldUpdateBufferSizeCorrectlyForSkippedRecords() {
        final PartitionGroup group = new PartitionGroup(
            logContext,
            mkMap(mkEntry(partition1, queue1)),
            tp -> OptionalLong.of(0L),
            getValueSensor(metrics, lastLatenessValue),
            enforcedProcessingSensor,
            maxTaskIdleMs
        );
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17299: add unit tests for previous fix (#17919).

**Test**
Test modified in PartitionGroupTest.java covering the corrected behaviour.
