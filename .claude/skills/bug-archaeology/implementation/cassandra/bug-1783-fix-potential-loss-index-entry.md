# Bug 1783: Fix potential loss of 2ndary index entry during compaction

**Commit**: 0191b359fc · **Ticket**: CASSANDRA-6517
**Subsystem**: db/compaction (PrecompactedRow)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

During compaction, `PrecompactedRow` notified secondary indexes of overwritten columns by calling `indexer.remove(column)`. The condition checked whether a column was superseded by a range tombstone using `returnCF.deletionInfo().isDeleted(column.name(), CompactionManager.NO_GC)`, which only compared against the column name with a gc-grace threshold. The correct check is `isDeleted(column)`, which uses the column's actual timestamp. Passing just the name could miss columns that were genuinely live (not deleted) or include columns that should not be removed from the index, causing secondary index entries to be incorrectly dropped during compaction.

### Before / After

```java
// BEFORE
|| returnCF.deletionInfo().isDeleted(column.name(), CompactionManager.NO_GC)
```

```java
// AFTER
|| returnCF.deletionInfo().isDeleted(column)
```

---

## How to Spot

When checking whether a mutation is covered by a deletion, always pass the full column (including its timestamp) rather than just its name or key. A deletion check that only uses the name ignores timestamp ordering and will produce incorrect results for columns that share a name but differ in timestamp.

---

## Test

A `RangeTombstoneTest` case was added that verifies secondary index entries are retained after compaction when the column is not actually covered by a range tombstone.
