# Bug 0664: AssertionError in Short Read Protection Counter Ordering

**Commit**: a7cb009f8a · **Ticket**: CASSANDRA-13747
**Subsystem**: src/java/org/apache/cassandra/service/DataResolver.java · db/transform/
**Category**: Logic error in condition

---

## Root Cause

Short read protection's `ShortReadRowProtection.moreContents()` relies on a `DataLimits.Counter` that has already been applied to the current partition to know how many rows have been consumed so far. The old code called `mergeAndFilter` which eagerly discarded empty partitions before the counter saw them, causing the counter to have counted zero rows (`x == 0`) when the formula `(n*n)/x - n` was evaluated — a division-by-zero producing an `AssertionError` (wrapped as an arithmetic exception rather than the intended positive retry count). Additionally, the empty-partition discard logic ran inside `Filter.applyToPartition`, which meant the counter transformation was applied to already-filtered partitions that could be empty.

The correct pipeline order is: merge → filter tombstones → apply row counter → discard empty partitions. The fix separates these steps and also guards the division with `Math.max(x, 1)`.

### Before / After

```java
// BEFORE — empty partitions discarded before counter sees them
DataLimits.Counter counter = command.limits().newCounter(command.nowInSec(), true, ...);
return counter.applyTo(mergeWithShortReadProtection(iters, sources, counter));
// mergeWithShortReadProtection internally called mergeAndFilter which discarded empties
```

```java
// AFTER — counter applied before empty-partition discard
DataLimits.Counter counter = ...;
UnfilteredPartitionIterator merged = mergeWithShortReadProtection(iters, sources, counter);
FilteredPartitions filtered = FilteredPartitions.filter(merged, new Filter(command.nowInSec()));
PartitionIterator counted = counter.applyTo(filtered);
return command.isForThrift() ? counted : Transformation.apply(counted, new EmptyPartitionsDiscarder());

// Also fix division by zero in SRP retry count formula
int toQuery = Math.max(((n * n) / Math.max(x, 1)) - n, 1);
```

---

## How to Spot

When a pipeline has multiple dependent transformation stages, check whether each stage's assumptions about prior state can be violated by reordering. Here, short read protection math depended on the counter already having observed some rows, but the preceding step could produce an empty partition count of zero. Any formula that divides by a counter derived from iterator consumption is vulnerable to a zero-division if an earlier transformation discards all rows before the counter runs.

---

## Test

No dedicated test added in this commit; the fix is validated by the existing SRP integration tests plus a new `ShortReadProtectionRequests` metric for observability.
