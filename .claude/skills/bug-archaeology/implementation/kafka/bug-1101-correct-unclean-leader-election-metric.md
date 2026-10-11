**Commit**: 3094ce2c20 · **Ticket**: KAFKA-19212
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
The current ElectionWasClean checks if the new leader is in the previous
ISR. However, there is a corner case in the partition reassignment. The
partition reassignment can change the partition replicas. If the new
preferred leader (the first one in the new replicas) is the last one to
join ISR, this preferred leader will be elected in the same partition
change.

**Before/After**
```java
// Before
                log.trace("Setting new leader for topicId {}, partition {} to {} using ELR",
                        topicId, partitionId, electionResult.node);
                log.info("Setting new leader for topicId {}, partition {} to {} using an unclean election",
                    topicId, partitionId, electionResult.node);
    private int electionFromElrCounter = 0;
                handlePartitionChange(prev.partitions().get(partitionId), nextPartition);
            // take current all replicas as ISR if prev is null (new created partition), so we won't treat it as unclean election.
            int[] prevIsr = prev != null ? prev.isr : next.replicas;
            int[] prevElr = prev != null ? prev.elr : new int[]{};
            if (!PartitionRegistration.electionWasClean(next.leader, prevIsr, prevElr)) {
                uncleanLeaderElection++;
            }

// After
                log.info("Setting new leader for topicId {}, partition {} to {} using ELR. Previous partition: {}, change record: {}",
                        topicId, partitionId, electionResult.node, partition, record);
                log.info("Setting new leader for topicId {}, partition {} to {} using an unclean election. Previous partition: {}, change record: {}",
                    topicId, partitionId, electionResult.node, partition, record);
    private int electionFromElr = 0;
    public int uncleanLeaderElection() {
        return uncleanLeaderElection;
    }

    public int electionFromElr() {
        return electionFromElr;
    }
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19212: correct the unclean leader election metric calculation (#19590).

**Test**
Test modified in ControllerMetricsChangesTest.java covering the corrected behaviour.
