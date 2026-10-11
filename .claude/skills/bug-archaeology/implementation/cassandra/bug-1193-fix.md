# Bug 1193: PartitionUpdate.rowCount Returns Stale Count Before Deduplication Sort

**Commit**: 87ea6bc913 · **Ticket**: CASSANDRA-9746
**Subsystem**: src/java/org/apache/cassandra/db/partitions/PartitionUpdate.java
**Category**: Missing override of default method

---

## Root Cause

`PartitionUpdate` accumulates rows in insertion order and defers sorting and deduplication to a lazy `maybeSort()` call. The base class `AbstractPartitionData.rowCount()` simply returns the raw `rows` counter, which reflects the number of rows before duplicate clustering keys are merged. `PartitionUpdate` overrode the iterator to call `maybeSort()` first, but failed to override `rowCount()` with the same guard, so callers that checked `rowCount()` before iterating would see an inflated count that included not-yet-merged duplicates.

### Before / After

```java
// BEFORE — no override; base class returns raw unsorted count
// (AbstractPartitionData)
public int rowCount()
{
    return rows;
}
```

```java
// AFTER — PartitionUpdate overrides to sort first
@Override
public int rowCount()
{
    maybeSort();
    return super.rowCount();
}
```

---

## How to Spot

When a class lazily normalises its state (sort, dedup, flush) before some operations but not others, check every accessor that depends on the normalised state for a matching guard. A missing override of a base-class method is easy to overlook during refactors; grep for every public method of the parent that reads state the subclass mutates lazily.

---

## Test

`RowUpdateBuilderTest` (renamed from `RowBuilderTest`) covers a two-row mutation where both rows share the same partition key and exercises `rowCount` indirectly through the applied result.
