**Commit**: ff5025a21c · **Ticket**: KAFKA-19695
**Subsystem**: share-coordinator/src/main/java/org/apache/kafka/coordinator/share/ShareCoordinatorOffsetsManager.java
**Category**: State not cleaned up

**Root cause**
`ShareCoordinatorOffsetsManager` tracked the last-known offset for every `SharePartitionKey` to compute which log offsets were redundant and could be cleaned up. When a share partition was deleted (tombstone record with `value == null`), the key's offset entry was added to the map but never removed. Since no further records would ever be written for a deleted topic's partition, the deleted key's offset would permanently anchor the `lastRedundantOffset` calculation, preventing subsequent log compaction from reclaiming space. The fix removes the key from the map after processing a tombstone, recognising it as a terminal state.

**Before/After**
```java
// Before: deleted keys left in the offsets map, anchoring redundant offset calc
public void updateState(SharePartitionKey key, long offset) {
    lastRedundantOffset.set(Math.min(lastRedundantOffset.get(), offset));
    offsets.put(key, offset);
    Optional<Long> redundantOffset = findRedundantOffset();
    redundantOffset.ifPresent(lastRedundantOffset::set);
}

// After: tombstone keys removed so they don't block redundant offset advancement
public void updateState(SharePartitionKey key, long offset, boolean isDelete) {
    ...
    if (isDelete) {
        offsets.remove(key);
    }
}
```

**How to spot**
In any "offset tracker" or "watermark" data structure, verify that tombstone / delete records are handled as terminal events that remove the associated entry. If a key remains in a tracking map after its lifecycle has ended, it will permanently cap any aggregation that depends on the minimum of all tracked values.

**Test**
`testUpdateStateAddsToInternalState` now calls `updateState(KEY1, 25L, true)` and asserts that `lastRedundantOffset` advances to `15L` (the remaining key's offset) and that `curState().get(KEY1)` is null.
