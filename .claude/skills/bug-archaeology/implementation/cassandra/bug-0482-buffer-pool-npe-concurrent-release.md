# Bug 0482: Buffer Pool NPE with Concurrent Release During Tiny Pool Eviction

**Commit**: fb9e74a4fe · **Ticket**: CASSANDRA-15726
**Subsystem**: `src/java/org/apache/cassandra/utils/memory/BufferPool.java`
**Category**: Race condition / missing synchronization

---

## Root Cause

The `TinyPool.removeIf()` method in `BufferPool` released matching chunks inline — calling `chunk.release()` while the pool's `count` and slot fields were still being adjusted. A concurrent thread releasing a buffer could re-enter `removeIf` (via the eviction path) and encounter null slots that had not yet been compacted to the back of the array, causing a `NullPointerException` when the predicate was tested against a `null` chunk. The invariant that nulls always occupy the highest-indexed slots was violated during the window between zeroing a slot and compacting the array.

### Before / After

```java
// BEFORE — chunk.release() called while nulls may be scattered
case 1:
    if (predicate.test(chunk0, value)) {
        --count;
        Chunk chunk = chunk0;
        chunk0 = null;
        chunk.release();   // re-entrant call can see null in middle slots
    }
```

```java
// AFTER — defer release until after null-compaction is complete
try {
    // mark slots null, decrement count, record in toRelease0/1/2
    // then compact nulls to high slots
} finally {
    if (toRelease0 != null) toRelease0.release();
    if (toRelease1 != null) toRelease1.release();
    if (toRelease2 != null) toRelease2.release();
}
```

---

## How to Spot

When a data structure's invariants (here: nulls-at-back) must hold for re-entrant callbacks to be safe, any code that breaks those invariants before triggering a callback is a hazard. Look for patterns where an object is nulled out and then immediately released/closed in the same statement; if that release can recursively call back into the same method, the intermediate state is exposed. Deferring releases to a `finally` block after all structural updates are complete is the standard fix.

---

## Test

No test added.
