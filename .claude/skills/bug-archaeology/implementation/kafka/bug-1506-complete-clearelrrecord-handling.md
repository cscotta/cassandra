**Commit**: fdbed6c458 · **Ticket**: KAFKA-18649
**Subsystem**: metadata/src
**Category**: State not cleaned up

**Root cause**
Implement ClearElrRecord handling in the TopicDelta. Also, the ReplicationControlManager should not merge updates if ELR/LastKnownElr are empty, becuase that will cause an unnecessary partition epoch bump. Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
            PartitionRegistration nextPartition = partition.merge(
                new PartitionChangeRecord().
                    setPartitionId(partitionId).
                    setTopicId(topic.id).
                    setEligibleLeaderReplicas(Collections.emptyList()).
                    setLastKnownElr(Collections.emptyList()));
            if (!nextPartition.equals(partition)) {
                topic.parts.put(partitionId, nextPartition);

// After
            if (partition.elr.length != 0 || partition.lastKnownElr.length != 0) {
                topic.parts.put(partitionId, partition.merge(
                    new PartitionChangeRecord().
                        setPartitionId(partitionId).
                        setTopicId(topic.id).
                        setEligibleLeaderReplicas(Collections.emptyList()).
                        setLastKnownElr(Collections.emptyList())));
import org.apache.kafka.common.metadata.ClearElrRecord;
            case CLEAR_ELR_RECORD:
                replay((ClearElrRecord) record);
                break;
    public void replay(ClearElrRecord record) {
```

**How to spot**
When reviewing metadata/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18649: complete clearelrrecord handling (#18708).

**Test**
Test modified in ReplicationControlManagerTest.java covering the corrected behaviour.
