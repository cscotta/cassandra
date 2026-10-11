# Bug 1087: columns() Returns Full Schema Columns Instead of Actual Partition Columns

**Commit**: c3bc85641b · **Ticket**: CASSANDRA-10220
**Subsystem**: src/java/org/apache/cassandra/db/partitions/AbstractBTreePartition.java
**Category**: Wrong constant / default value

---

## Root Cause

`AbstractBTreePartition.columns()` always returned `metadata.partitionColumns()`—the full set of columns defined in the schema—rather than the set of columns actually present in the partition's data. This caused callers (read paths, iterators, serializers) to believe a partition contained every schema column even when only a subset had been written. The fix moves the `PartitionColumns` reference from the `AbstractBTreePartition` instance field into the `Holder` value object, so the columns are recorded at build time from the actual iterator's `columns()` and survive atomic swaps of the holder in `AtomicBTreePartition`. The `PartitionUpdate.columns()` override is added separately to avoid triggering a premature build when columns are queried before the row tree is assembled.

### Before / After

```java
// BEFORE — always returns the entire schema column set
public PartitionColumns columns()
{
    // We don't really know which columns will be part of the update, so assume it's all of them
    return metadata.partitionColumns();
}
```

```java
// AFTER — returns the columns recorded when the partition was built
public PartitionColumns columns()
{
    return holder().columns;
}
// Holder now carries: PartitionColumns columns; (set from iterator.columns() at build time)
```

---

## How to Spot

Anywhere a `columns()` method is documented with a comment like "assume all columns" or "we don't really know," it is returning a safe over-approximation that may be incorrect in practice. Such methods should be audited to determine whether the true column set is available at construction time (via an `UnfilteredRowIterator` or `RowIterator`) and stored in the data holder. The symptom of returning too many columns is rarely an assertion failure; it usually manifests as unnecessary column fetches, wrong metadata in serialized frames, or incorrect behaviour in code that switches on column presence.

---

## Test

No test added.
