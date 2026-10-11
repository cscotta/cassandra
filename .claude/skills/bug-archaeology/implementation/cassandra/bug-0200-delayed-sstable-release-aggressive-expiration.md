# Bug 0200: Delayed SSTable Release with unsafe_aggressive_sstable_expiration

**Commit**: 87c2af85c1 · **Ticket**: CASSANDRA-18756
**Subsystem**: db/compaction (CompactionController, TWCS)
**Category**: Logic error in condition

---

## Root Cause

`CompactionController.refreshOverlaps()` called `ignoreOverlaps()` before TWCS had a chance to set its own state, because `ignoreOverlaps()` is a virtual method overridden in `TimeWindowCompactionStrategy`-spawned controllers but the base-class constructor (and `refreshOverlaps`) invoked it while the subclass was still being set up. As a result, when `ignoreOverlaps()` returned `true` (because TWCS sets that flag lazily), the controller skipped acquiring references to overlapping SSTables entirely instead of just skipping the periodic re-check. Overlapping SSTables that should have been tracked were never held by reference, so they weren't counted as compaction participants and could not be promptly released after the compaction window expired.

### Before / After

```java
// BEFORE – skipped acquiring overlapping refs entirely when ignoreOverlaps() was true
if (compacting == null || ignoreOverlaps())
    overlappingSSTables = Refs.tryRef(Collections.<SSTableReader>emptyList());
else
    overlappingSSTables = cfs.getAndReferenceOverlappingLiveSSTables(compacting);
```

```java
// AFTER – always acquire refs; only skip the periodic refresh log message
if (compacting == null)
    overlappingSSTables = Refs.tryRef(Collections.<SSTableReader>emptyList());
else
    overlappingSSTables = cfs.getAndReferenceOverlappingLiveSSTables(compacting);
```

---

## How to Spot

When a virtual method controls whether a data-structure initialization step runs, and that method is called from the base-class constructor or from a method that can be invoked before the subclass finishes its own initialization, the check may return a stale/default value at construction time. Review every call-site of an overridable method in constructors or in helpers called from constructors to ensure the subclass invariant already holds at that point; add a comment or Javadoc warning that the method must not be called until initialization is complete.

---

## Test

Byteman-instrumented test with two concurrent compactions: verifies that `getAndReferenceOverlappingLiveSSTables` is called exactly twice for the first compaction (once at construction and once after a refresh), regardless of whether `unsafe_aggressive_sstable_expiration` is enabled.
