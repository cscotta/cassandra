# Bug 1837: Fix Setting Last Compacted Key in the Wrong Level for LCS

**Commit**: 02a93025e1 · **Ticket**: CASSANDRA-6284
**Subsystem**: src/java/org/apache/cassandra/db/compaction
**Category**: Logic error in condition

---

## Root Cause

`LeveledManifest.replace()` records the highest key reached during a compaction in `lastCompactedKeys[minLevel]` so the next compaction for that level starts beyond the last compacted point (round-robin within a level). The `minLevel` was computed by iterating the *added* SSTables (the new output) and taking the minimum of their levels. However, output SSTables may be promoted to a higher level; what matters for scheduling is the *input* level — the level from which data was compacted. By computing `minLevel` from inputs (`removed`) instead of outputs (`added`), the correct source level is recorded.

### Before / After

```java
// BEFORE — minLevel derived from added (output) sstables, wrong when promoted
int minLevel = Integer.MAX_VALUE;
for (SSTableReader ssTableReader : added) {
    minLevel = Math.min(minLevel, ssTableReader.getSSTableLevel());
    add(ssTableReader);
}
lastCompactedKeys[minLevel] = SSTable.sstableOrdering.max(added).last;
```

```java
// AFTER — minLevel derived from removed (input) sstables
int minLevel = Integer.MAX_VALUE;
for (SSTableReader sstable : removed) {
    int thisLevel = remove(sstable);
    minLevel = Math.min(minLevel, thisLevel);
}
for (SSTableReader ssTableReader : added)
    add(ssTableReader);
lastCompactedKeys[minLevel] = SSTable.sstableOrdering.max(added).last;
```

---

## How to Spot

When tracking "progress within a level", always key the tracking state on the *source* level from which rows were read, not the *destination* level they are promoted into. Review any code that mixes input and output SSTables in the same bookkeeping loop.

---

## Test

No test added.
