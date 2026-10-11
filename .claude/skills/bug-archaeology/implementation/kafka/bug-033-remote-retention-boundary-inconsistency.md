**Commit**: e26e7c06e7 · **Ticket**: KAFKA-20091
**Subsystem**: storage/src/main/java/org/apache/kafka/server/log/remote/storage/RemoteLogManager.java
**Category**: Incorrect filtering / wrong query result

**Root cause**
The local-segment retention check uses a strict `>` comparison (`currentTime - largestTimestamp > retentionMs`), meaning a segment is not deleted when the difference equals `retentionMs` exactly. The remote-segment counterpart used `<=` (`maxTimestampMs <= cleanupUntilMs`) which deleted segments exactly at the boundary, creating an inconsistency: a segment that local retention would keep could be deleted by remote retention for the same policy. Changing the remote check to `<` aligns both paths.

**Before/After**
```java
// Before – deletes at the boundary (inconsistent with local path)
shouldDeleteSegment = metadata.maxTimestampMs() <= retentionTimeData.get().cleanupUntilMs;

// After – consistent with local "strictly older" semantics
shouldDeleteSegment = metadata.maxTimestampMs() < retentionTimeData.get().cleanupUntilMs;
```

**How to spot**
Whenever two code paths implement the "same" policy, compare their boundary conditions carefully. A `<=` vs `<` mismatch in expiry/retention checks can silently delete data that should be preserved.

**Test**
`RemoteLogManagerTest#testDeletionOnRetentionBreachedSegments` is extended with an `expectDeletion=false` case that sets `maxTimestampMs == cleanupUntilMs` and asserts that `deleteLogSegmentData` is never called, confirming the boundary is now exclusive.
