# Bug 1330: CommitLog forceRecycleAllSegments Calls close() Recursively Causing Memory Access Error

**Commit**: 36dc512ad4 · **Ticket**: CASSANDRA-8812
**Subsystem**: src/java/org/apache/cassandra/db/commitlog/CommitLogSegment.java
**Category**: Logic error in condition

---

## Root Cause

`CommitLogSegment.sync()` contained a call to `close()` when it needed to finalize and release a segment. The `close()` method was not `synchronized` and did not discard the unused tail or wait for in-flight modifications before releasing memory. When `forceRecycleAllSegments()` triggered this code path, the buffer could still be accessed by a concurrent writer after the memory was freed. The fix introduces a two-level close design: the public `close()` is now `synchronized` and performs all necessary pre-close housekeeping, then delegates to the raw `internalClose()` which performs the actual file release; `sync()` calls `internalClose()` directly to avoid re-doing that housekeeping.

### Before / After

```java
// BEFORE
if (close)
    close();  // not synchronized, no tail discard, no wait for modifications
```

```java
// AFTER
if (close)
    internalClose();  // raw close; caller (sync) has already handled housekeeping

// New public close():
synchronized void close() {
    discardUnusedTail();
    waitForModifications();
    lastSyncedOffset = buffer.capacity();
    internalClose();
}
```

---

## How to Spot

When a method named `close()` is called from within the same class in a path that is not the normal close lifecycle, it is worth verifying whether that call is safe — especially if the class manages native or memory-mapped memory. A `close()` that performs both bookkeeping and resource release is dangerous to call mid-operation; splitting it into a guarded public entry point and an internal raw release method is a standard pattern to prevent accidental double-processing or premature release.

---

## Test

No test added.
