# Bug 0697: Secondary Index on Primary Key Column Creates Expiring Entry

**Commit**: b0db519b79 · **Ticket**: CASSANDRA-13412
**Subsystem**: src/java/org/apache/cassandra/db/index/AbstractSimplePerColumnSecondaryIndex.java
**Category**: Logic error in condition

---

## Root Cause

When a secondary index is built on a primary key column (partition key or clustering key), write operations that include a TTL on a non-primary-key column should not propagate that TTL to the index entry for the primary key column. Primary key columns cannot themselves expire, so creating an `ExpiringCell` in the secondary index for such a column causes the index entry to disappear after the TTL elapses — even though the row is still alive. The condition that checked `cell instanceof ExpiringCell` did not exclude primary key columns, so any write to the row that touched an expiring non-key column would install a time-limited index entry for a non-expiring key column.

### Before / After

```java
// BEFORE
if (cell instanceof ExpiringCell)
{
    ExpiringCell ec = (ExpiringCell) cell;
    cfi.addColumn(new BufferExpiringCell(name, ...));
}
```

```java
// AFTER
if (!columnDef.isPrimaryKeyColumn() && cell instanceof ExpiringCell)
{
    ExpiringCell ec = (ExpiringCell) cell;
    cfi.addColumn(new BufferExpiringCell(name, ...));
}
```

---

## How to Spot

Whenever index-entry creation logic branches on the cell type to copy TTL metadata, check whether the indexed column is a primary key column. Primary key columns are immutable and cannot expire, so copying a TTL observed on a sibling cell in the same write batch into the index entry for the key column is always wrong. A reviewer should ask: "could the write that triggered this path carry a TTL from a non-key column that is being incorrectly applied to a key column's index entry?"

---

## Test

Tests insert a row with a composite primary key, add a secondary index on a key column, update a non-key column with `TTL 1`, sleep for 1 second, and assert that the indexed row is still returned via the index query after expiry of the non-key column.
