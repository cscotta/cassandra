**Commit**: 5d66f8d88c · **Ticket**: KAFKA-17035
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
      startMs - segment.largestTimestamp > retentionMs
      if (diff - segment.size >= 0) {
        diff -= segment.size
        true
      } else {
        false
      nextSegmentOpt.exists(_.baseOffset <= (if (remoteLogEnabled()) localLogStartOffset() else logStartOffset))

// After
      val shouldDelete = startMs - segment.largestTimestamp > retentionMs
      debug(s"$segment retentionMs breached: $shouldDelete, startMs=$startMs, retentionMs=$retentionMs")
      shouldDelete
      val segmentSize = segment.size
      val shouldDelete = diff - segmentSize >= 0
      debug(s"$segment retentionSize breached: $shouldDelete, log size before delete segment=$diff, after delete segment=${diff - segmentSize}")
      if (shouldDelete) {
        diff -= segmentSize
      shouldDelete
      val isRemoteLogEnabled = remoteLogEnabled()
      val localLSO = localLogStartOffset()
      val shouldDelete = nextSegmentOpt.exists(_.baseOffset <= (if (isRemoteLogEnabled) localLSO else logStartOffset))
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17035 add debug log to retention cleanuplogs method to help troubleshoot issues (#16790).

**Test**
No test added.
