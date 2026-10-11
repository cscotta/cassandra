**Commit**: 7311fe52a3 · **Ticket**: KAFKA-19929
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/ShareConsumeRequestManager.java
**Category**: Logic error in condition

**Root cause**
`ShareConsumeRequestManager.maximumTimeToWait` was overridden to return `0` (no wait) whenever `record_limit` mode was active, records were needed, partitions were assigned, and no fetch node had been chosen yet. The reasoning was that the manager should immediately retry node selection, but this misunderstood the polling loop: returning `0` caused the event loop to spin continuously even when no progress was possible, wasting CPU and potentially interfering with other request managers. The fix removes the override entirely, letting the default `Long.MAX_VALUE` apply so the poller sleeps until an event arrives.

**Before/After**
```java
// Before – returns 0 when no node chosen in record_limit mode
@Override
public long maximumTimeToWait(long currentTimeMs) {
    if (isShareAcquireModeRecordLimit() && fetchMoreRecords
            && subscriptions.numAssignedPartitions() > 0
            && fetchRecordsNodeId.get() == -1) {
        return 0L;
    }
    return Long.MAX_VALUE;
}

// After – method removed entirely; default Long.MAX_VALUE used
```

**How to spot**
Overrides of `maximumTimeToWait` that return `0` under any common condition cause busy-polling. The method should only return a short value when there is a known future event that the manager needs to wake up for at a specific time.

**Test**
No dedicated unit test added for this change; the commit is a targeted removal of incorrect logic.
