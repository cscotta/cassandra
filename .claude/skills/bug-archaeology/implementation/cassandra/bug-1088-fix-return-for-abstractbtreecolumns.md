# Bug 1088: Fix return of 'columns()' for AbstractBTreeColumns

**Commit**: 520089b52f · **Ticket**: CASSANDRA-10220
**Subsystem**: db/partitions/AbstractBTreePartition.java
**Category**: Missing override of default method

---

## Root Cause

`AbstractBTreePartition.columns()` returned `metadata.partitionColumns()` — the full schema-defined column set — instead of the actual columns present in the partition's data. The `PartitionColumns` descriptor was stored as a field on the abstract class and set at construction time, but it was never updated as data was merged in, and the field itself was not authoritative: several callers passed inconsistent values. The fix moves `columns` into the `Holder` snapshot (the same object that carries the BTree, deletion info, and stats), so the column set evolves atomically with the data and every path that builds or merges a `Holder` records the actual columns from the iterator or update.

### Before / After

```java
// BEFORE
public PartitionColumns columns()
{
    // We don't really know which columns will be part of the update, so assume it's all of them
    return metadata.partitionColumns();
}
```

```java
// AFTER
public PartitionColumns columns()
{
    return holder().columns;
}
```

---

## How to Spot

When a method that should reflect the *current* state of a mutable data structure instead returns a schema-level or construction-time constant, the comment "we don't really know" is a strong signal that the value is a placeholder rather than a genuine answer. Review every place the field is set and ask whether it is updated on every mutating path; if writes to the field and writes to the underlying data are not co-located in a single atomic unit (here, the `Holder` record), the two can diverge.

---

## Test

No test added.
