**Commit**: 83dc0d7eae · **Ticket**: KAFKA-19862
**Subsystem**: group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroup.java (and StreamsGroup.java)
**Category**: State not cleaned up

**Root cause**
During group coordinator loading, compaction can remove the record that unassigns a partition/task before the coordinator has processed it. When the coordinator then tries to call `removePartitionEpochs` or `removeTaskProcessIds` for a partition that is no longer tracked, it threw an `IllegalStateException`. The records self-resolve by the time loading completes, so the exception was spurious. The fix degrades these conditions from fatal exceptions to debug-level log warnings, allowing loading to continue.

**Before/After**
```java
// Before – throws on missing epoch during load
Integer prevValue = partitionsOrNull.remove(partitionId);
if (prevValue != expectedEpoch) {
    throw new IllegalStateException("Cannot remove the epoch ...");
}

// After – logs and continues
Integer prevValue = partitionsOrNull.get(partitionId);
if (prevValue != null && prevValue == expectedEpoch) {
    partitionsOrNull.remove(partitionId);
} else {
    log.debug("[GroupId {}] Cannot remove the epoch ...", ...);
}
```

**How to spot**
Loading/replay code that assumes a strictly-ordered event log will fail when compaction skips intermediate records. Throw-on-inconsistency is appropriate for runtime mutations but not for log replay, where gaps are expected.

**Test**
New unit tests for `ConsumerGroup` and `StreamsGroup` verify that loading with a missing un-assignment record completes successfully instead of throwing.
