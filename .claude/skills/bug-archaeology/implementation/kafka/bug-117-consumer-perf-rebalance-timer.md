**Commit**: 60ad638a35 · **Ticket**: KAFKA-19617
**Subsystem**: tools / ConsumerPerformance
**Category**: Race condition / missing synchronization

**Root cause**
`ConsumerPerfRebListener.onPartitionsRevoked()` recorded the rebalance start time unconditionally every time it was called. With the newer cooperative rebalance protocol, only some partitions may be revoked at a time (incremental rebalance), so `onPartitionsRevoked` can fire without a corresponding full stop. The subsequent `onPartitionsAssigned` call then computed a spurious elapsed time from a start timestamp that reflected a partial revocation, corrupting the join-time metric. The fix tracks which partitions are currently assigned and only starts the timer when all assigned partitions have been revoked (i.e., the set is empty).

**Before/After**
```java
// Before — unconditional timer reset on any revocation
public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
    joinStartMs = System.currentTimeMillis();
}

// After — timer reset only when no partitions remain assigned
public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
    assignedPartitions.removeAll(partitions);
    if (assignedPartitions.isEmpty()) {
        joinStartMs = System.currentTimeMillis();
    }
}
```

**How to spot**
Metrics computed as `(assigned_time - revoked_time)` are correct only when the revocation and assignment are "total" (all partitions). Under incremental/cooperative rebalance, partial revocations happen without a corresponding full assignment; the timer must only fire when the entire partition set has been cleared.

**Test**
`ConsumerPerformanceTest.testConsumerListenerWithPartialPartitionRevokedAndAssigned` — assigns two partitions, revokes one, assigns the same one back; asserts `joinTimeMs` did not change. `testConsumerListenerWithAllPartitionRevokedAndAssigned` — revokes all, waits 100 ms, assigns new partitions; asserts `joinTimeMs` did increase.
