# Bug 1899: Columns Silently Skipped When Using Multiple Slices in IndexedSliceReader

**Commit**: 20a805023f · **Ticket**: CASSANDRA-6119
**Subsystem**: src/java/org/apache/cassandra/db/columniterator
**Category**: Silent skip on non-match

---

## Root Cause

`IndexedSliceReader` uses a `column` variable to hold the last-read atom while it searches for the correct slice. When a column fell before the current slice start it was discarded with `continue`, but when it was within a slice it was added and then fetched a new column on the next loop iteration — so far so good. However, when the column was past the current slice end, the code advanced to the next slice and looped, but it had already consumed the column from the file without re-examining it against the new slice, silently dropping it. The fix holds the column across loop iterations and only fetches a new one from disk when the current column has been fully processed.

### Before / After

```java
// BEFORE — column consumed regardless of whether it was handled
for (int i = 0; i < columns; i++) {
    OnDiskAtom column = atomSerializer.deserializeFromSSTable(...);
    if (!inSlice && isColumnBeforeSliceStart(column))
        continue;        // column dropped, but not added
    if (isColumnBeforeSliceFinish(column)) {
        inSlice = true;
        addColumn(column);
    }
    // else: column past slice end — NOT re-examined for next slice
}
```

```java
// AFTER — column retained until it is either added or definitively past all slices
OnDiskAtom column = null;
int i = 0;
while (i < columns || column != null) {
    if (column == null) {
        column = atomSerializer.deserializeFromSSTable(...);
        i++;
    }
    if (!inSlice && isColumnBeforeSliceStart(column)) {
        column = null;
        continue;
    }
    if (isColumnBeforeSliceFinish(column)) {
        inSlice = true;
        addColumn(column);
        column = null;
    }
    // else: advance slice, re-examine same column on next iteration
}
```

---

## How to Spot

In any iterator that reads ahead from a stream, ensure that a record which fails a filter predicate is either put back or kept for re-evaluation rather than silently discarded. Whenever a loop `continue`s after reading but before storing, ask whether the read item should have been retained for the next predicate check.

---

## Test

Four new unit tests added in `ColumnFamilyStoreTest`: `testMultiRangeSomeEmptyNoIndex`, `testMultiRangeSomeEmptyIndexed`, `testMultiRangeContiguousNoIndex`, and `testMultiRangeContiguousIndexed`. Each writes columns to a flushed SSTable, issues a multi-slice `SliceQueryFilter`, and asserts the exact set of returned column names.
