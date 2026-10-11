# Bug 1358: IndexedSliceReader Drops Range Tombstones and Loops Infinitely When Slices Exhausted

**Commit**: e16d76dc84 · **Ticket**: n/a (fixup for CASSANDRA-8558)
**Subsystem**: src/java/org/apache/cassandra/db/columniterator/IndexedSliceReader.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The original fix for CASSANDRA-8558 in `IndexedSliceReader` introduced two problems in `fetchWithIndex`. First, when the prefetch queue contained cells that fell after the last slice, the code called `prefetched.poll()` one cell at a time and discarded range tombstones instead of collecting them into `blockColumns`. This silently dropped range tombstones that were needed to correctly shadow deleted data. Second, after draining the prefetch queue when no more slices existed, the outer loop did not break out; it fell through to the disk-read path and could loop indefinitely. The fix drains the entire prefetch queue in one shot while preserving range tombstones, and adds an explicit `return false` path when both `blockColumns` is empty and no more slices remain.

### Before / After

```java
// BEFORE — discards range tombstones one cell at a time, no early-exit when slices exhausted
if (hasMoreSlice())
    setNextSlice();
else
    prefetched.poll();   // drops RangeTombstone silently
```

```java
// AFTER — drains queue collecting all range tombstones, then breaks
if (!setNextSlice())
{
    while ((prefetchedCol = prefetched.poll()) != null)
        if (prefetchedCol instanceof RangeTombstone)
            blockColumns.addLast(prefetchedCol);
    break;
}
```

---

## How to Spot

Whenever a loop processes a mixed stream of regular cells and range tombstones, every branch that discards "unwanted" cells must check the runtime type and preserve tombstones. A bare `poll()` without a type check is a red flag in any iterator that is supposed to be tombstone-aware. Additionally, after a loop that drains a queue, always verify that the method has an explicit termination path; falling through to an unconditional fetch loop is a common source of infinite loops in multi-phase iterators.

---

## Test

No test added. This commit is a follow-up fixup to a prior patch and relies on the test coverage introduced with CASSANDRA-8558.
