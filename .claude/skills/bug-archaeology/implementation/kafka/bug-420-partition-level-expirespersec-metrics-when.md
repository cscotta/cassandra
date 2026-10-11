**Commit**: e4119eb399 · **Ticket**: KAFKA-20204
**Subsystem**: checkstyle/import-control-storage.xml
**Category**: Logic error in condition

**Root cause**
Partition-level `ExpiresPerSec` metrics in `DelayedProduceMetrics` and
`DelayedRemoteListOffsets` are never removed, leading to metric
accumulation over time. This patch adds `removePartitionMetrics` to both classes and calls it
from `ReplicaManager` in two paths:    - `stopPartitions()`, partition
deletion and controlled shutdown. - `applyLocalFollowersDelta()`,
leader-to-follower transitions. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        completeDelayedOperationsWhenNotPartitionLeader(topicPartition, partition.topicId)}

// After
            <allow pkg="kafka.utils" />

  def removePartitionMetrics(partition: TopicPartition): Unit = {
    if (partitionExpirationMeters.remove(partition) != null) {
      metricsGroup.removeMetric("ExpiresPerSec",
        Map("topic" -> partition.topic, "partition" -> partition.partition.toString).asJava)
    }
  }
      // Clean up per-partition expiration metrics regardless of whether the local log
      // is deleted. This covers both partition deletion and reassignment (leader -> follower).
      DelayedProduceMetrics.removePartitionMetrics(topicPartition)
      DelayedRemoteListOffsets.removePartitionMetrics(topicPartition)
```

**How to spot**
When reviewing checkstyle/import-control-storage.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20204 remove partition-level `expirespersec` metrics when the broker no longer owns the partition (#21573).

**Test**
Test modified in DelayedProduceTest.scala covering the corrected behaviour.
