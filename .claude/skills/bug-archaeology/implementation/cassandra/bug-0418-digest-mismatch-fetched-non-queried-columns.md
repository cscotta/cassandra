# Bug 0418: Digest Mismatch Between Memtable and SSTable for Fetched-But-Not-Queried Columns

**Commit**: a9ab7bde4e · **Ticket**: CASSANDRA-15962
**Subsystem**: db/rows (read path / digest queries)
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When `Cell.Serializer.deserialize` reads a cell that is fetched (needed for liveness purposes) but not actually queried by the user, it stores an empty byte buffer as the cell value to save memory.  `BTreeRow.filter()` and `ComplexColumnData.filter()` did not apply the same transformation when filtering in-memory (memtable) rows: they returned the cell with its full value intact for non-queried columns.  As a result, a digest query on data still in the memtable computed a different digest than the same query on the same data after it had been flushed to an SSTable, causing spurious `DigestMismatchException` errors and unnecessary read repairs.

### Before / After

```java
// BEFORE – BTreeRow.filter: non-queried cell kept with real value
boolean isSkippable = !queriedByUserTester.test(column) && cell.timestamp() < rowLiveness.timestamp();
return isForDropped || isShadowed || isSkippable ? null : cell;
```

```java
// AFTER – non-queried cell has value blanked to match SSTable deserialization behaviour
boolean isSkippable = !queriedByUserTester.test(column);
if (isForDropped || isShadowed || (isSkippable && cell.timestamp() < rowLiveness.timestamp()))
    return null;
return isSkippable ? cell.withSkippedValue() : cell;
```

---

## How to Spot

Any place that filters or transforms cells for the read path must stay in sync with `Cell.Serializer.deserialize`'s handling of `fetchedCellIsQueried`.  When reviewing `Row.filter()` or `ComplexColumnData.filter()`, check that the value replacement logic (`withSkippedValue`) is applied in the in-memory path whenever it would be applied by the SSTable deserializer; divergence will show up only in digest queries and only while data is still in the memtable.

---

## Test

New `SSTableAndMemTableDigestMatchTest` exercises digest computation with various `ColumnFilter` selections (all columns, no columns, specific simple columns, map cells) and asserts that the digest is identical before and after a flush.
