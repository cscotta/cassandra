**Commit**: b3d0093252 · **Ticket**: KAFKA-19995
**Subsystem**: server/src/main/java/org/apache/kafka/server/log/remote/storage/RemoteLogManager.java (RLMCopyTask)
**Category**: Logic error in condition

**Root cause**
`RLMCopyTask.recordLagStats()` was called only after a successful copy, meaning any exception thrown during the copy path caused the lag metrics to go unrecorded for that iteration. Additionally, the lag calculation `onlyLocalLogSegmentsSize() - activeSegment().size()` could produce a negative value after a leader change (when `onlyLocalLogSegmentsSize()` returns 0 but the active segment has a non-zero size), corrupting the metric. The fix wraps `recordLagStats` in a `finally` block and adds `Math.max(0, ...)` guards.

**Before/After**
```java
// BEFORE – lag not recorded on exception; can go negative
try {
    // ... copy logic ...
    recordLagStats(log);
} catch (Exception e) {
    recordLagStats(log);   // duplicated, and the outer try swallows it
    throw e;
}

// AFTER – always recorded, never negative
} finally {
    recordLagStats(log);
}

void recordLagStats(UnifiedLog log) {
    long bytesLag  = Math.max(0, log.onlyLocalLogSegmentsSize() - log.activeSegment().size());
    long segsLag   = Math.max(0, log.onlyLocalLogSegmentsCount() - 1);
    recordLagStats(bytesLag, segsLag);
}
```

**How to spot**
Metric recording code that lives inside the happy path of a `try` block will miss recording on exception. Any metric that represents a running gauge (lag, pending count) should be updated in a `finally` block. Also check subtraction-based lag formulas that can silently go negative after leadership transitions.

**Test**
Three new unit tests: `testCopyLagMetricsWithOnlyActiveSegment`, `testCopyLagMetricsWithMultipleSegments`, and `testCopyLagMetricsAfterLeaderChangeWithHigherRemoteOffset`, each mocking the log and asserting specific metric values including the 0-floor edge case.
