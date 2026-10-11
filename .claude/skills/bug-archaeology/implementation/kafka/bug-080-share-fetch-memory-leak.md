**Commit**: 997abe464f · **Ticket**: KAFKA-19389
**Subsystem**: core/src/main/java/kafka/server/share/DelayedShareFetch.java
**Category**: Resource leak

**Root cause**: A `DelayedShareFetch` operation is registered under two purgatory key types: `DelayedShareFetchGroupKey` and `DelayedShareFetchPartitionKey`. When the operation completed and triggered `onComplete`, it called `checkAndComplete` only for the group key. The operation object (which holds a reference to the entire fetched record batch) remained pinned in the purgatory's watched-key index for the partition key until the background purge task ran. With the default purge interval this could hold large message batches in memory long enough to cause broker OOM. The fix adds a matching `checkAndComplete` call for the partition key immediately after completing on the group key.

**Before/After**

```java
// Before – only group key triggered
replicaManager.addToActionQueue(() -> topicIdPartitions.forEach(topicIdPartition ->
    replicaManager.completeDelayedShareFetchRequest(
        new DelayedShareFetchGroupKey(...))));

// After – both keys triggered
replicaManager.addToActionQueue(() -> topicIdPartitions.forEach(topicIdPartition -> {
    replicaManager.completeDelayedShareFetchRequest(
        new DelayedShareFetchGroupKey(...));
    replicaManager.completeDelayedShareFetchRequest(
        new DelayedShareFetchPartitionKey(topicIdPartition));
}));
```

**How to spot**: Whenever a delayed operation is registered under multiple purgatory keys, every completion path must call `checkAndComplete` (or equivalent) for all keys. Leaving any key unconsumed keeps the operation—and its payload—alive until the purge cycle.

**Test**: No dedicated unit test added for the leak itself; tested manually against a tiered-storage cluster where broker OOM occurred after ~8040 messages before the fix and no longer recurs after.
