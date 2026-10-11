# Bug 1245: Fix canonical view returning early opened SSTables

**Commit**: 7d6a60ddd0 · **Ticket**: CASSANDRA-9396
**Subsystem**: src/java/org/apache/cassandra/db/ColumnFamilyStore.java
**Category**: Logic error in condition

---

## Root Cause

The `CANONICAL_SSTABLES` view in `ColumnFamilyStore` is supposed to return exactly one representative copy of each data file — specifically, it should exclude "early-opened" SSTableReaders, which are partially-written cursors created mid-compaction for streaming purposes. The original code added all compacting SSTables unconditionally (via `addAll`) and only filtered out early-open readers from the non-compacting set. This meant that an early-open SSTableReader in the compacting set was always included in the canonical view, exposing callers (e.g. repair, streaming) to an incomplete, in-progress data file.

### Before / After

```java
// BEFORE – early-open readers in compacting set bypass the filter
sstables.addAll(view.compacting);
for (SSTableReader sstable : view.sstables)
    if (!view.compacting.contains(sstable) && sstable.openReason != SSTableReader.OpenReason.EARLY)
        sstables.add(sstable);
```

```java
// AFTER – the same EARLY guard is applied to compacting SSTables as well
for (SSTableReader sstable : view.compacting)
    if (sstable.openReason != SSTableReader.OpenReason.EARLY)
        sstables.add(sstable);
for (SSTableReader sstable : view.sstables)
    if (!view.compacting.contains(sstable) && sstable.openReason != SSTableReader.OpenReason.EARLY)
        sstables.add(sstable);
```

---

## How to Spot

When a filter predicate is applied to one branch of a two-set union but not the other, consider whether the predicate should apply universally. Any time a "canonical" or "representative" view is built by merging two collections with a filtering step on only one, read the logic as two separate loops and ask whether the invariant (here: "no EARLY readers") must hold for both. The asymmetry between `addAll` and a filtered `for` loop is a clear signal that the predicate coverage is incomplete.

---

## Test

`SSTableRewriterTest.testCanonicalView()` was added. It writes an SSTable, starts a compaction with a forced early-open interval, and asserts mid-compaction that the canonical view contains exactly one non-early SSTableReader.
