**Commit**: 4c5ea05ec8 · **Ticket**: KAFKA-18058
**Subsystem**: coordinator-common/src
**Category**: Logic error in condition

**Root cause**
In this PR, we've added a class ShareCoordinatorOffsetsManager, which tracks the last redundant offset for each share group state topic partition. We have also added a periodic timer job in ShareCoordinatorService which queries for the redundant offset at regular intervals and if a valid value is found, issues the deleteRecords call to the ReplicaManager via the PartitionWriter. In this way the size of the partitions is kept manageable. Reviewers: Jun Rao <junrao@gmail.com>, David Jacot <djacot@confluent.io>, Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import java.util.Collections;

    /**
     * Util method which returns all the topic partitions for which
     * the state machine is in active state.
     * <p>
     * This could be useful if the caller does not have a specific
     * target internal topic partition.
     * @return List of {@link TopicPartition} whose coordinators are active
     */
    public List<TopicPartition> activeTopicPartitions() {
        if (coordinators == null || coordinators.isEmpty()) {
```

**How to spot**
When reviewing coordinator-common/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18058: share group state record pruning impl. (#18014).

**Test**
Test modified in InMemoryPartitionWriter.java covering the corrected behaviour.
