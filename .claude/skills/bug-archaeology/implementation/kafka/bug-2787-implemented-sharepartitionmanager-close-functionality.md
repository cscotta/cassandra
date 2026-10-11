**Commit**: 93dd9acad0 · **Ticket**: KAFKA-17003
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
About
Implemented close() functionality of SharePartitionManager to avoid any chances of memory leak. The functionality will be utilized when the sharePartitionObject is closed, at the time when BrokerServer is killed. Testing
Added unit tests to cover the new functionality added. Reviewers:  Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    SharePartitionManager(
        // TODO: Provide Implementation
            return new SharePartitionManager(replicaManager, time, cache, partitionCacheMap, RECORD_LOCK_DURATION_MS, MAX_DEL

// After
    private SharePartitionManager(
    // Visible for testing.
    SharePartitionManager(
            ReplicaManager replicaManager,
            Time time,
            ShareSessionCache cache,
            Map<SharePartitionKey, SharePartition> partitionCacheMap,
            int recordLockDurationMs,
            Timer timer,
            int maxDeliveryCount,
            int maxInFlightMessages,
            Persister persister
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17003: implemented sharepartitionmanager close() functionality (#16431).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
