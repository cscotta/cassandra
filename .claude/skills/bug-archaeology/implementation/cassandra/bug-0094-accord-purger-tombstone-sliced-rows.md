# Bug 0094: Accord Compaction Purger Tombstone Deletes Latest Durability Value on Sliced Rows

**Commit**: 10e065ffe5 · **Ticket**: CASSANDRA-18795
**Subsystem**: db/compaction — CompactionIterator (Accord commands purger)
**Category**: Logic error in condition

---

## Root Cause

During compaction, the Accord commands purger only receives a random slice of command state (because compaction input files are chosen randomly). When a row is determined to need truncation, the purger was emitting a row-level tombstone at the `ERASE` case. However, if the compacted slice did not include the `durability` column (because it was in a different SSTable not selected for this compaction run), the tombstone could permanently delete the latest durability value, leaving the command in a state where it could never again be determined to be universally durable. Additionally, the `TRUNCATE`/`TRUNCATE_WITH_OUTCOME` logic was zeroing out columns by resetting them to sentinel values (`Timestamp.NONE`, `NO_LAST_EXECUTED_HLC`) and then re-writing them, instead of simply dropping the cell from the output row—creating unnecessary tombstones that could again mask later higher-value cells. The fix makes `ERASE` emit a proper row deletion tombstone (so the key is cleared uniformly), uses `maybeDropTruncatedCommandColumns` (which drops cells without emitting tombstones) for the `TRUNCATE` path, and adds a fast-path guard using `Cleanup.isSafeToCleanup` that lets universally-durable slices be dropped immediately without needing all columns present.

### Before / After

```java
// BEFORE — ERASE simply returns null (no tombstone for partial rows)
case ERASE:
    return null;

// BEFORE — TRUNCATE re-writes zeroed sentinel columns (creates tombstones)
case TRUNCATE:
    return truncatedApply(cleanup.appliesIfNot, row, nowInSec, cleanup == TRUNCATE_WITH_OUTCOME);
```

```java
// AFTER — ERASE emits a proper row deletion so sliced siblings are also wiped
case ERASE:
    return BTreeRow.emptyDeletedRow(row.clustering(),
        new Row.Deletion(DeletionTime.build(row.primaryKeyLivenessInfo().timestamp(), nowInSec), false));

// AFTER — TRUNCATE drops unneeded cells without tombstones
case TRUNCATE:
    if (saveStatus.compareTo(cleanup.appliesIfNot) >= 0)
        return maybeDropTruncatedCommandColumns(row, cleanup == TRUNCATE_WITH_OUTCOME, ...);
    return truncatedApply(cleanup.appliesIfNot, row, nowInSec, durability, ...);
```

---

## How to Spot

Whenever a compaction purger processes partial rows (rows that represent only a subset of columns because compaction inputs are randomly selected), returning `null` to discard a row is incorrect—it only discards what is present in this compaction run; columns in other SSTables remain. The safe operation is to emit a row-level tombstone timestamped at the row's own liveness timestamp, which will shadow all column cells in future reads and compactions. Review every `return null` in a compaction purger that operates on mutable, multi-column Accord state.

---

## Test

`CompactionAccordIteratorsTest` was substantially extended with new cases covering partial-row slices and tombstone emission.
