# Bug 0772: Query Timeout Not Checked Inside Partitions for Range Queries

**Commit**: 84b1725fb4 · **Ticket**: CASSANDRA-13050
**Subsystem**: Read path / Query monitoring
**Category**: Logic error in condition

---

## Root Cause
`CheckForAbort` (the transformation that enforces query timeouts) only acted at the partition boundary: `applyToPartition` checked whether to abort, but never applied itself to the rows *within* each partition. For single-partition reads this was harmless because there was only one partition, but range queries iterate many partitions and each partition can contain a large number of rows. Once `applyToPartition` passed a partition without applying the transformation to its rows, no per-row abort check ran for that partition's lifetime, making query monitoring effectively dead for the inner loop of range scans.

The fix makes `applyToPartition` chain the `CheckForAbort` transformation onto the row iterator it returns (`Transformation.apply(partition, this)`), so the timeout is also checked row-by-row inside every partition. It also adds an `ApproximateTime`-based short-circuit so the check does not call through to `MonitorableImpl` on every single row when the clock hasn't advanced.

A secondary issue: `SinglePartitionReadCommand` was wrapping its inner iterators in an additional `withStateTracking` call that was no longer needed (and possibly harmful) after this restructuring; those calls were removed.

### Before / After
```java
// BEFORE: only partition boundary checked, rows inside partition never checked
protected BaseRowIterator<?> applyToPartition(BaseRowIterator<?> partition)
{
    if (maybeAbort())
    {
        partition.close();
        return null;
    }
    return partition;  // no transformation applied to rows inside
}

// AFTER: transformation applied to rows inside the partition too
protected UnfilteredRowIterator applyToPartition(UnfilteredRowIterator partition)
{
    if (maybeAbort())
    {
        partition.close();
        return null;
    }
    return Transformation.apply(partition, this);  // row-level checking enabled
}
```

---

## How to Spot
When a `Transformation` needs to act at both the partition level and the row level, `applyToPartition` must call `Transformation.apply(partition, this)` to register itself for row callbacks as well — simply returning the partition unmodified skips all inner callbacks. Review every `StoppingTransformation` or `Transformation` subclass to confirm it re-applies itself to the partition iterator when per-row side-effects are required.

---

## Test
`RepairedDataTombstonesTest` updated (minor adjustments); no dedicated monitoring test added.
