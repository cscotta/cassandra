# Bug 1477: Missing `break` Causes IllegalStateException in Anticompaction for Non-Intersecting Ranges

**Commit**: 838151caba · **Ticket**: CASSANDRA-7892
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Logic error in condition

---

## Root Cause

In `CompactionManager.performAnticompaction`, SSTables are matched against repair ranges to determine whether they need anticompaction. When an SSTable's range does not intersect any of the repair ranges it is added to `nonAnticompacting` and removed from the iterator. However, without a `break` statement after this action, the loop continued to the next repair range and attempted further processing on the already-removed SSTable. This caused an `IllegalStateException` because the iterator had already removed the element, making subsequent operations on it illegal.

### Before / After

```java
// BEFORE
logger.info("SSTable {} ({}) does not intersect repaired range {}, not touching repairedAt.",
            sstable, sstableRange, r);
nonAnticompacting.add(sstable);
sstableIterator.remove();
// (no break — loop continues to next range)
```

```java
// AFTER
logger.info("SSTable {} ({}) does not intersect repaired range {}, not touching repairedAt.",
            sstable, sstableRange, r);
nonAnticompacting.add(sstable);
sstableIterator.remove();
break;
```

---

## How to Spot

In nested loops where an iterator removes an element and the element should no longer be processed, always add a `break` (or `continue` on the outer loop) immediately after the remove. Review any `iterator.remove()` call inside an inner loop to confirm the outer iteration is correctly terminated.

---

## Test

`shouldSkipAntiCompactionForNonIntersectingRange` in `AntiCompactionTest`: creates a store with keys "0"–"9", runs anticompaction against a range `[-10, -1]` that does not intersect any key, and asserts that the single SSTable remains unrepaired and no `IllegalStateException` is thrown.
