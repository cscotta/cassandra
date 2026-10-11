**Commit**: 2d39d5be64 · **Ticket**: KAFKA-17783
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>, Jun Rao <junrao@gmail.com>

**Before/After**
```java
// Before
        GroupConfigManager groupConfigManager
            timer, time, persister, replicaManager, groupConfigManager, SharePartitionState.EMPTY);
        SharePartitionState sharePartitionState
                            groupConfigManager
        SharePartition sharePartition = partitionCacheMap.remove(sharePartitionKey);
        if (sharePartition != null) {
            sharePartition.markFenced();
        }

// After
import kafka.server.share.SharePartitionManager.SharePartitionListener;
    /**
     * The listener is used to notify the share partition manager when the share partition state changes.
     */
    private final SharePartitionListener listener;

        GroupConfigManager groupConfigManager,
        SharePartitionListener listener
            timer, time, persister, replicaManager, groupConfigManager, SharePartitionState.EMPTY, listener);
        SharePartitionState sharePartitionState,
        SharePartitionListener listener
        this.listener = listener;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17783: adding listeners to remove share partition on partition changes (#17796).

**Test**
Test modified in SharePartitionManagerTest.java covering the corrected behaviour.
