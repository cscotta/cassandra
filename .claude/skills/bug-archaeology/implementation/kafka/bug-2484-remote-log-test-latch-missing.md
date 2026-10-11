**Commit**: ead9ed513e · **Ticket**: KAFKA-17422
**Subsystem**: storage/RemoteLogManagerTest
**Category**: Race condition / missing synchronization

**Root cause**
In `RemoteLogManagerTest`, the test launched a background task that copies a log segment and then checked expiration behaviour. The `copyLogSegmentLatch.countDown()` call was missing after the expiration task path completed, so the test's `await()` would block for the full 5-second timeout before proceeding. This made the test artificially slow and could mask ordering issues. The fix adds the missing `countDown()` in all exit paths of the mocked copy task.

**Before/After**
```java
// Before: latch never counted down on expiration path
doAnswer(invocation -> {
    // ... set up copy ...
    return null; // missing countDown on this path
}).when(remoteStorageManager).copyLogSegment(...);

// After: countDown in all paths
doAnswer(invocation -> {
    try { /* copy logic */ }
    finally { copyLogSegmentLatch.countDown(); }
    return null;
}).when(remoteStorageManager).copyLogSegment(...);
```

**How to spot**
When a test uses a `CountDownLatch` to synchronize with a mocked async operation, verify that every code path inside the mock answer—including exception and early-return paths—calls `countDown()`. A missing `countDown` on any branch causes the test to wait for the full timeout, hiding the real assertion.

**Test**
`RemoteLogManagerTest` corrected so the latch is always decremented; test runtime reduced from ~5 s to near-instant.
