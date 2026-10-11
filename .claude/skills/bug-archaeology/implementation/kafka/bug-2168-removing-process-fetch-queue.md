**Commit**: 7efbed4e49 · **Ticket**: KAFKA-17545
**Subsystem**: core/src
**Category**: Race condition / missing synchronization

**Root cause**
The PR removed the process fetch queue as we have moved to share fetch purgatory. Reviewers: Abhinav Dixit <adixit@confluent.io>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
    /**
     * The fetch queue stores the share fetch requests that are waiting to be processed.
     */
    private final ConcurrentLinkedQueue<ShareFetchData> fetchQueue;

    /**
     * The process fetch queue lock is used to ensure that only one thread is processing the fetch queue at a time.
     */
    private final AtomicBoolean processFetchQueueLock;


// After
        this(replicaManager,
            time,
            cache,
            partitionCacheMap,
            defaultRecordLockDurationMs,
            new SystemTimerReaper("share-group-lock-timeout-reaper",
                new SystemTimer("share-group-lock-timeout")),
            maxDeliveryCount,
            maxInFlightMessages,
            persister,
            groupConfigManager,
            metrics
```

**How to spot**
When reviewing core/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17545: removing process fetch queue (#17534).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
