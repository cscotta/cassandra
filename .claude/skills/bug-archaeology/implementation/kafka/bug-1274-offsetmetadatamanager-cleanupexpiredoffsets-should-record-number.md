**Commit**: 5d2bfb4151 · **Ticket**: KAFKA-18980
**Subsystem**: group-coordinator/src
**Category**: State not cleaned up

**Root cause**
jira: https://issues.apache.org/jira/browse/KAFKA-18980

We should use the number of `records` rather than topic partitions,but
this is not a bug as the number of `records` should be equal to the
number of topic partitions. Also, we previously used `expiredPartitions` for logging, but now it is
not being used anymore, so we can remove it. Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.HashSet;
import java.util.Set;
        Set<String> expiredPartitions = new HashSet<>();
                        expiredPartitions.add(appendOffsetCommitTombstone(groupId, topic, partition, records).toString());
        metrics.record(OFFSET_EXPIRED_SENSOR_NAME, expiredPartitions.size());
     *
     * @return The topic partition of the corresponding tombstone.
    private TopicPartition appendOffsetCommitTombstone(
        TopicPartition tp = new TopicPartition(topic, partition);
        log.trace("[GroupId {}] Removing expired offset and metadata for {}", groupId, tp);
        return tp;

// After
                        appendOffsetCommitTombstone(groupId, topic, partition, records);
        metrics.record(OFFSET_EXPIRED_SENSOR_NAME, records.size());
    private void appendOffsetCommitTombstone(
        log.trace("[GroupId {}] Removing expired offset and metadata for {}-{}", groupId, topic, partition);
```

**How to spot**
When reviewing group-coordinator/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18980 offsetmetadatamanager#cleanupexpiredoffsets should record the number of records rather than topic partitions.

**Test**
No test added.
