# Bug 0863: BufferCell constructor allows primary key column, assert added; test fixed to use partition column

**Commit**: 9659a389ca · **Ticket**: n/a
**Subsystem**: Storage Engine / Row representation (BufferCell)
**Category**: Missing null / bounds check

---

## Root Cause
`BufferCell` is the in-memory representation of a non-primary-key cell, but its constructor had no guard against being constructed with a `ColumnDefinition` that belongs to the primary key. The `RowIndexEntryTest` accidentally exercised this invalid path by calling `cfMeta.allColumns().iterator().next()` to obtain a column, which could return a partition key column — an illegal input for `BufferCell`. The fix adds an assertion `!column.isPrimaryKeyColumn()` to `BufferCell`'s constructor and corrects the test to use `cfMeta.partitionColumns().iterator().next()`, which returns only non-primary-key columns.

### Before / After
```java
// BEFORE (BufferCell.java constructor — no guard)
public BufferCell(ColumnDefinition column, long timestamp, int ttl, int localDeletionTime, ByteBuffer value, CellPath path)
{
    super(column);
    assert column.isComplex() == (path != null);
    ...
}

// AFTER
public BufferCell(ColumnDefinition column, long timestamp, int ttl, int localDeletionTime, ByteBuffer value, CellPath path)
{
    super(column);
    assert !column.isPrimaryKeyColumn();
    assert column.isComplex() == (path != null);
    ...
}
```

---

## How to Spot
Any factory method or constructor that wraps a `ColumnDefinition` should validate the column kind before proceeding. Callers that obtain a column by iterating `allColumns()` include primary key columns; downstream consumers that only handle regular/static columns must use a narrower accessor such as `partitionColumns()` or `regularAndStaticColumns()`. Code review should question any use of `allColumns()` outside of schema-introspection contexts.

---

## Test
`RowIndexEntryTest` is the regression test; the existing test was corrected to use `partitionColumns()` so that it no longer passes a primary key column to `BufferCell.live()`. The new assertion in `BufferCell` will catch any future misuse at test time.
