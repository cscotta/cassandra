# Bug 1340: Early Compaction Replacement Leaves One-Key Overlap Between New and Source SSTables

**Commit**: 857ee0ac37 · **Ticket**: CASSANDRA-8683
**Subsystem**: src/java/org/apache/cassandra/db/DataTracker.java, io/sstable/SSTableReader.java, io/sstable/SSTableRewriter.java
**Category**: Race condition / missing synchronization

---

## Root Cause

During compaction with early SSTable opening, `SSTableRewriter.moveStarts()` advanced the start key of still-compacting source SSTables to exactly the last key written by the new early-opened file. This left the last key of the new file also present in the source SSTables' range, creating a one-key overlap. Concurrent reads could therefore return duplicate rows, or reads could be routed to the wrong file. The fix introduces a `SHADOWED` open reason and a separate `shadowed` set in `DataTracker.View`, so fully-covered source SSTables are removed from the interval tree (and thus from query routing) while their reference counts are still managed safely.

### Before / After

```java
// BEFORE — moveStarts used newStart == reader.last, causing one-key overlap
moveStarts(reader, Functions.constant(reader.last), false);
// source SSTable's new start was set to reader.last, so both files covered that key
```

```java
// AFTER — lowerbound is exclusive; source SSTables that are fully shadowed
// are moved to a separate set and excluded from the interval tree
moveStarts(reader, reader.last, false);
// inside moveStarts: if lowerbound >= latest.last, clone as SHADOWED
if (lowerbound.compareTo(latest.last) >= 0)
    replacement = latest.cloneAsShadowed(runOnClose);
else {
    DecoratedKey newStart = latest.firstKeyBeyond(lowerbound);
    replacement = latest.cloneWithNewStart(newStart, runOnClose);
}
```

```java
// BEFORE — View had no concept of shadowed SSTables; interval tree included them
View(... Set<SSTableReader> sstables, Set<SSTableReader> compacting, SSTableIntervalTree intervalTree)

// AFTER — shadowed set tracked separately; interval tree built only from sstables
View(... Set<SSTableReader> sstables, Set<SSTableReader> compacting,
         Set<SSTableReader> shadowed, SSTableIntervalTree intervalTree)
```

---

## How to Spot

Whenever a "move start" or "advance lower bound" operation is applied to source files during incremental replacement, verify whether the boundary is inclusive or exclusive. An off-by-one at the boundary means both the old and new file contain the boundary key, producing duplicates. Any time a new open-reason variant is introduced for SSTables, check that the interval tree construction explicitly excludes those readers.

---

## Test

`SSTableRewriterTest` updated to assert zero overlap between early-opened and compacting SSTables, and to verify the `shadowed` set is non-empty during compaction and empty after completion.
