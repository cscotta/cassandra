**Commit**: 8cf969e00a · **Ticket**: KAFKA-18734
**Subsystem**: core/src
**Category**: Incorrect filtering / wrong query result

**Root cause**
The PR implements the SharePartitionMetrics as defined in KIP-1103, with
one change. The metric `FetchLockRatio` is defined as `Meter` in KIP but
is implemented as `HIstogram`. There was a discussion about same on
KIP-1103 discussion where we thought that `FetchLockRatio` is
pre-aggregated but while implemeting the rate from `Meter` can go above
100 as `Meter` defines rate per time period. Hence it makes more sense
to implement metric `FetchLockRatio` as `Histogram`.

**Before/After**
```java
// Before
            timer, time, persister, replicaManager, groupConfigManager, SharePartitionState.EMPTY, listener);
        SharePartitionListener listener

        lock.readLock().lock();
        long numRecords;
        try {
            if (cachedState.isEmpty()) {
                numRecords = 0;
            } else {
                numRecords = this.endOffset - this.startOffset + 1;
            }
        } finally {

// After
import org.apache.kafka.server.share.metrics.SharePartitionMetrics;
@SuppressWarnings("ClassDataAbstractionCoupling")
    /**
     * The share partition metrics is used to track the broker-side metrics for the share partition.
     */
    private final SharePartitionMetrics sharePartitionMetrics;

    /**
     * The fetch lock acquired time is used to track the time when the lock for share partition is acquired.
     */
    private long fetchLockAcquiredTimeMs;

```

**How to spot**
When reviewing core/src code, look for incorrect filtering / wrong query result patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18734: implemented share partition metrics (kip-1103) (#19045).

**Test**
Test modified in SharePartitionTest.java covering the corrected behaviour.
