**Commit**: 1962917436 · **Ticket**: KAFKA-17674
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            CompletableFuture<Void> refreshWithCommittedOffsets = initWithCommittedOffsetsIfNeeded(deadlineMs);
            updatePositions = refreshWithCommittedOffsets.thenCompose(__ -> initWithPartitionOffsetsIfNeeded());
            updatePositions = initWithPartitionOffsetsIfNeeded();
     * If there are partitions still needing a position and a reset policy is defined, request reset using the
     * default policy.
    private CompletableFuture<Void> initWithPartitionOffsetsIfNeeded() {
            subscriptionState.resetInitializingPositions();
    private CompletableFuture<Void> initWithCommittedOffsetsIfNeeded(long deadlineMs) {
        final Set<TopicPartition> initializingPartitions = subscriptionState.initializingPartitions();

import org.apache.kafka.clients.consumer.KafkaConsumer;

// After
        final Set<TopicPartition> initializingPartitions = subscriptionState.initializingPartitions();
            CompletableFuture<Void> refreshWithCommittedOffsets = initWithCommittedOffsetsIfNeeded(initializingPartitions, deadlineMs);
            updatePositions = refreshWithCommittedOffsets.thenCompose(__ -> initWithPartitionOffsetsIfNeeded(initializingPartitions));
            updatePositions = initWithPartitionOffsetsIfNeeded(initializingPartitions);
     * If there are partitions still needing a position and a reset policy is defined, request reset using the default policy.
     * @param initializingPartitions Set of partitions that should be initialized. This won't reset positions for
     *                               partitions that may have been added to the subscription state, but that are not
     *                               included in this set.
    private CompletableFuture<Void> initWithPartitionOffsetsIfNeeded(Set<TopicPartition> initializingPartitions) {
            subscriptionState.resetInitializingPositions(initializingPartitions::contains);
     * @param initializingPartitions Set of partitions to update with a position. This same set will be kept
     *                               throughout the whole process (considered when fetching committed offsets, and
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17674: fix bug on update positions of newly added partitions (#17342).

**Test**
Test modified in OffsetsRequestManagerTest.java covering the corrected behaviour.
