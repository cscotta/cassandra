# Bug 0240: SSTableUpgrade Processes SSTables in Undefined Order

**Commit**: 8df69c8016 · **Ticket**: CASSANDRA-18143
**Subsystem**: tools/StandaloneUpgrader, db/Directories
**Category**: Logic error in condition

---

## Root Cause

`StandaloneUpgrader` iterated over the map returned by `SSTableLister.list()`, which has no defined ordering because `list()` returns an `ImmutableMap` whose entry set order depends on insertion order (itself driven by directory traversal). When an sstable being upgraded references an older sstable (for example, via a linked component), upgrading the newer one first could fail or produce incorrect output because dependencies were not yet upgraded. The fix adds a `sortedList()` method to `SSTableLister` that sorts entries by `Descriptor.generation` before returning them, ensuring upgrades happen in chronological creation order.

### Before / After

```java
// BEFORE
for (Map.Entry<Descriptor, Set<Component>> entry : lister.list().entrySet())
```

```java
// AFTER
for (Map.Entry<Descriptor, Set<Component>> entry : lister.sortedList())
// sortedList() sorts by Descriptor.generation ascending
```

---

## How to Spot

Any loop that iterates over `SSTableLister.list().entrySet()` without sorting is implicitly relying on filesystem traversal order, which is not guaranteed. When the operation is order-sensitive (upgrades, sequential processing), always sort by generation. Review all callers of `SSTableLister.list()` to check whether they assume or require a particular order.

---

## Test

No dedicated test added; the fix is a deterministic ordering change whose correctness is validated by the upgrade integration tests.
