# Bug 0439: fetchedCellIsQueried Ignores Column-Level Query Status for Complex Columns

**Commit**: c3636bf599 · **Ticket**: CASSANDRA-15977
**Subsystem**: `src/java/org/apache/cassandra/db/filter/ColumnFilter.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`ColumnFilter.fetchedCellIsQueried(column, path)` checked only whether subselection state was present, without first verifying that the owning column itself was part of the queried (as opposed to merely fetched) set. For a `ColumnFilter` built with table metadata, complex columns are fetched for tombstone purposes even when not selected by the query. Without the column-level guard, cells from those unselected complex columns were incorrectly reported as queried, causing them to appear in results or digest computations when they should have been excluded.

### Before / After

```java
// BEFORE – skipped column-level queried check when isFetchAll was false
public boolean fetchedCellIsQueried(ColumnDefinition column, CellPath path) {
    assert path != null;
    if (!isFetchAll || subSelections == null)
        return true;
    // ...
}
```

```java
// AFTER – column-level queried check gates the cell-level check
public boolean fetchedCellIsQueried(ColumnDefinition column, CellPath path) {
    assert path != null;
    if (!fetchedColumnIsQueried(column))
        return false;
    if (subSelections == null)
        return true;
    // ...
}
```

---

## How to Spot

When a two-level containment check (column → cell) is implemented as separate methods, look for any method that performs the inner (cell) check without delegating to the outer (column) check first. In `ColumnFilter`, `fetches()`, `fetchedColumnIsQueried()`, and `fetchedCellIsQueried()` form a hierarchy; any caller or implementation that skips a level in that hierarchy will produce inconsistent results when the filter was built with table metadata that fetches columns for internal purposes.

---

## Test

`ColumnFilterTest.testFetchedQueried` covers all combinations of filter construction (with/without table metadata, selection builder, sub-selection) and asserts that `fetchedCellIsQueried` returns false for cells belonging to a complex column that was not included in the query selection.
