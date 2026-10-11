# Bug 0874: Race Condition Rebuilding CFMetaData.columnMetadata

**Commit**: dd05e46f0c · **Ticket**: CASSANDRA-12098
**Subsystem**: Schema / CFMetaData
**Category**: Race condition / missing synchronization

---

## Root Cause
`CFMetaData.columnMetadata` was declared as a `ConcurrentHashMap` and rebuilt by calling `clear()` followed by a series of `put()` calls. Even though `ConcurrentHashMap` is thread-safe for individual operations, the clear-then-repopulate sequence is not atomic: a reader thread that called `getColumnDefinition()` between `clear()` and the final `put()` would see an incomplete (partially-empty) map. Switching to a `volatile HashMap` replaced in one atomic reference assignment (`this.columnMetadata = newColumnMetadata`) eliminates the window between clearing the old data and making the new data visible.

### Before / After
```java
// BEFORE — non-atomic clear + repopulate
private final Map<ByteBuffer, ColumnDefinition> columnMetadata = new ConcurrentHashMap<>();

void rebuild() {
    this.columnMetadata.clear();          // readers see empty map here
    for (ColumnDefinition def : partitionKeyColumns)
        this.columnMetadata.put(def.name.bytes, def);
    // ... more puts ...
}

// AFTER — single volatile write, fully populated before publish
private volatile Map<ByteBuffer, ColumnDefinition> columnMetadata = new HashMap<>();

void rebuild() {
    Map<ByteBuffer, ColumnDefinition> newColumnMetadata = new HashMap<>();
    for (ColumnDefinition def : partitionKeyColumns)
        newColumnMetadata.put(def.name.bytes, def);
    // ... more puts into newColumnMetadata ...
    this.columnMetadata = newColumnMetadata;  // atomic publish
}
```

---

## How to Spot
Any "clear-and-refill" pattern on a shared collection — even a concurrent one — is a race window. The safe idiom is to build a new collection locally (no visibility to other threads) and publish it with a single volatile/atomic write. Review rebuilding methods that call `collection.clear()` and look for whether concurrent readers could observe the intermediate empty state.

---

## Test
No dedicated unit test added for this race; fixed by structural change to the rebuild logic.
