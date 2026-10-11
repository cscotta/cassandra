# Bug 021: Avoid re-initializing underlying iterator in LazilyInitializedUnfilteredRowIterator after closing

**Commit**: e721705152 · **Ticket**: CASSANDRA-20972
**Subsystem**: Storage engine / row iteration
**Category**: State not cleaned up

---

## Root Cause

`LazilyInitializedUnfilteredRowIterator` used `iterator == null` as the sole indicator of whether the iterator had been closed. The `close()` method set `iterator = null` after closing it. However, several callers (e.g., `DISTINCT` scans, per-partition-limit reads) call methods such as `isOpen()` or trigger `maybeInit()` on an already-closed iterator. Because `iterator` was `null`, `maybeInit()` would re-initialize the underlying SSTable iterator, re-opening I/O resources that had already been released and leading to incorrect reads or errors. The fix introduces a dedicated `closed` boolean flag so that the "not yet initialized" and "already closed" states are distinct.

### Before / After

```java
// BEFORE — null used for both "not yet opened" and "already closed"
public void close()
{
    if (iterator != null)
    {
        iterator.close();
        iterator = null;          // after this, maybeInit() would re-open
    }
}

public boolean isOpen()
{
    return iterator != null;
}

// AFTER — separate closed flag prevents re-initialization
private boolean closed = false;

public void close()
{
    closed = true;
    if (iterator != null)
        iterator.close();
    // iterator intentionally left non-null to avoid re-init
}

public boolean isOpen()
{
    if (closed)
        return false;
    return iterator != null;
}
```

---

## How to Spot

When a lazy wrapper uses a single nullable reference to represent both "not yet initialized" and "already closed", closing and then calling any method that checks that reference will silently re-initialize the resource. Reviewers should look for lazy-init patterns (`if (x == null) x = init()`) and verify that a separate lifecycle flag (or a terminal sentinel distinct from `null`) guards against post-close re-entry.

---

## Test

`DistinctReadTest.test` inserts a row behind a range tombstone, flushes, then executes `SELECT DISTINCT`, a filtered range scan, and a `PER PARTITION LIMIT 1` query — all of which previously threw exceptions or returned wrong results due to the re-initialization.
