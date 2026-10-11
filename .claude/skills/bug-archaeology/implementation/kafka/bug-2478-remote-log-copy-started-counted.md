**Commit**: ad4405c8dd · **Ticket**: KAFKA-17062
**Subsystem**: storage/RemoteLogManager
**Category**: Logic error in condition

**Root cause**
`RemoteLogManager` computed remote retention size by iterating all remote log segment metadata and summing segments whose state was not `DELETE_SEGMENT_STARTED`. This incorrectly included segments in the `COPY_SEGMENT_STARTED` state, which represent in-progress uploads that may never complete. Counting these inflated the apparent remote size and caused the manager to prematurely delete older segments to satisfy the retention limit. The fix changes the predicate to only count segments in `COPY_SEGMENT_FINISHED` or `DELETE_SEGMENT_STARTED` (the latter negated), and adds cleanup of dangling `COPY_SEGMENT_STARTED` segments on copy failure.

**Before/After**
```java
// Before: counts COPY_SEGMENT_STARTED as already-remote data
long remoteSize = segmentMetadata.stream()
    .filter(s -> s.state() != RemoteLogSegmentState.DELETE_SEGMENT_STARTED)
    .mapToLong(RemoteLogSegmentMetadata::segmentSizeInBytes)
    .sum();

// After: only count fully copied segments
long remoteSize = segmentMetadata.stream()
    .filter(s -> s.state() == RemoteLogSegmentState.COPY_SEGMENT_FINISHED)
    .mapToLong(RemoteLogSegmentMetadata::segmentSizeInBytes)
    .sum();
```

**How to spot**
When computing an aggregate over a collection of state-machine records, verify that transient or partial states are excluded. An in-progress state counted the same as a terminal state will cause premature resource release or incorrect quota enforcement.

**Test**
`RemoteLogManagerTest` updated to assert that in-progress segments are excluded from retention size calculations.
