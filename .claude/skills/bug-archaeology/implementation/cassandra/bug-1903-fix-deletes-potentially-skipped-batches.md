# Bug 1903: Deletes Potentially Skipped When Merging Batches in UnsortedColumns

**Commit**: 8f88670f72 · **Ticket**: CASSANDRA-6115
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: State not cleaned up

---

## Root Cause

`UnsortedColumns.addAll` iterated over and added all individual columns from the source `ColumnFamily`, but did not propagate the source's `DeletionInfo` (partition- or range-level tombstones). When a batch contained a delete operation, that deletion was encoded in `cm.deletionInfo()` rather than as a regular column, so it was silently lost during the merge. Adding `delete(cm.deletionInfo())` before the column loop ensures tombstone state is applied first.

### Before / After

```java
// BEFORE — DeletionInfo from source CM never applied
public void addAll(ColumnFamily cm, Allocator allocator,
                   Function<Column, Column> transformation) {
    for (Column column : cm)
        addColumn(column);
}
```

```java
// AFTER — DeletionInfo propagated before individual columns
public void addAll(ColumnFamily cm, Allocator allocator,
                   Function<Column, Column> transformation) {
    delete(cm.deletionInfo());
    for (Column column : cm)
        addColumn(column);
}
```

---

## How to Spot

Whenever implementing a "merge / addAll" method on a data structure that carries both data and metadata (e.g., tombstones, TTLs, flags), explicitly check that all metadata fields are transferred, not just the iterable data cells.

---

## Test

No test added.
