# Bug 0086: Static Segment Opened with Empty Offset When Active Segment Not Properly Flushed Before Switch

**Commit**: 00ebcdc9f2 · **Ticket**: CASSANDRA-19867
**Subsystem**: journal/ActiveSegment, journal/SyncedOffsets
**Category**: Non-atomic file write

---

## Root Cause

When an `ActiveSegment` was converted to a static (read-only) segment via `persistAndClose()`, the index and metadata were flushed to disk but the `SyncedOffsets` file was not. On crash-recovery or segment re-open, the static segment's offset file was empty (or reflected an earlier synced position) because the in-memory offset had never been written to the file. The fix adds an explicit `syncedOffsets.fsync()` call inside `persistAndClose()` and adds an `output.flush()` inside the offset-write path to ensure buffered bytes reach the OS before the fsync.

### Before / After

```java
// BEFORE – persistAndClose() does not flush syncedOffsets
index.persist(descriptor);
metadata.persist(descriptor);
SyncUtil.trySyncDir(descriptor.directory);
```

```java
// AFTER – syncedOffsets flushed before directory sync
index.persist(descriptor);
metadata.persist(descriptor);
syncedOffsets.fsync();
SyncUtil.trySyncDir(descriptor.directory);
```

---

## How to Spot

When a segment transition persists multiple related files (index, metadata, offsets), verify that every file is explicitly flushed and synced before the directory entry is synced. A missing flush on any one file leaves the segment in a partially-durable state that appears valid but contains stale data after recovery.

---

## Test

No test added.
