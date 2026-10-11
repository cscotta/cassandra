# Bug 0354: PrunableArrayQueue Left in Corrupt State When Pruner Throws

**Commit**: 2b6799a394 · **Ticket**: CASSANDRA-16866
**Subsystem**: src/java/org/apache/cassandra/net (PrunableArrayQueue)
**Category**: State not cleaned up

---

## Root Cause

`PrunableArrayQueue.prune()` iterated over the queue and compacted kept elements in-place, but if any of the `Pruner` callbacks (`shouldPrune`, `onPruned`, `onKept`) threw an exception the loop exited immediately via the uncaught throw, leaving the queue's backing array in a partially compacted state with null gaps.  The `head` field adjustment in the `finally` block was already present for the normal path, but calling code that caught the exception and continued to use the queue would then encounter null elements and incorrect size values.

### Before / After

```java
// BEFORE
if (pruner.shouldPrune(e))
{
    buffer[k] = null;
    removed++;
    pruner.onPruned(e);
}
else
{
    if (removed > 0)
        buffer[(k + removed) & mask] = e;
    pruner.onKept(e);
}
// (exception from any pruner callback exits loop immediately, leaving nulls in buffer)
```

```java
// AFTER
// Each pruner callback is wrapped in try/catch; first error is saved.
// If error != null, shouldPrune is skipped (always keep) but compaction continues.
// After the loop, head is corrected and the saved error is rethrown.
if (error != null)
    throw Throwables.unchecked(error);
```

---

## How to Spot

Any in-place compaction loop that modifies a shared data structure while calling user-supplied callbacks needs to guarantee the structure is valid before propagating exceptions.  The pattern "do compaction work; call callback" means a callback failure mid-loop leaves the structure in a half-compacted state.  The fix is to separate structural work from callback invocation and always complete structural cleanup in `finally` before rethrowing.

---

## Test

Randomized test using an `UnreliablePruner` that throws from `shouldPrune`, `onPruned`, or `onKept` at random; after the pruning attempt (which may throw) the test drains the queue with `poll()` and asserts no null elements are returned and `size()` reaches zero.
