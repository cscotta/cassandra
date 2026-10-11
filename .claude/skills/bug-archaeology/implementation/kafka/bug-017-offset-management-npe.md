**Commit**: 8b47aa813a · **Ticket**: KAFKA-17411
**Subsystem**: streams — RocksDBStore / ProcessorStateManager / GlobalStateManagerImpl
**Category**: Missing null / bounds check

**Root cause**
Several interrelated issues in the new offset-management path: (1) `RocksDBStore.commit(offsets)` did not guard against a `null` value in the offsets map, causing an NPE when `ProcessorStateManager` passed `null` for a partition with no committed offset; (2) `LegacyCheckpointingStateStore` and `RocksDBStore` behaved differently for `null` offsets and empty maps; (3) `GlobalStateManagerImpl` did not wipe state under EOS when corruption was detected, unlike `ProcessorStateManager`; (4) several delegating stores (`RocksDBTimestampedStore`, window/session stores) did not implement `managesOffsets()` / `committedOffset()`, causing them to be incorrectly wrapped in `LegacyCheckpointingStateStore`, conflicting with the RocksDB-internal offset tracking.

**Before/After**
```java
// Before — NPE if null offset passed to commit
public void commit(Map<TopicPartition, Long> offsets) {
    offsets.forEach((tp, offset) -> writeOffset(tp, offset));  // NPE when offset==null
}

// After — null offset treated as "wipe this partition's offset"
// empty map treated as "wipe all offsets" (corruption recovery)
```

**How to spot**
When introducing a new offset-management mechanism that replaces checkpoint files, audit all implementors of `StateStore.commit()` for null-safety and empty-map semantics. Also verify that every delegating store that tracks offsets internally implements the `managesOffsets()` guard so it is not double-wrapped.

**Test**
Several integration tests updated (`EosIntegrationTest`, `GlobalKTableEOSIntegrationTest`). Two EOS integration tests that depended on reading from `.checkpoint` files were removed because offsets are now stored in RocksDB and not externally readable. The remaining tests exercise the new code paths including corruption-triggered store wipe.
