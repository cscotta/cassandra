# Bug 1534: Fix Min/Max Cell Name Collection with 2.0 Range Tombstones

**Commit**: c46477b8e3 · **Ticket**: CASSANDRA-7593
**Subsystem**: src/java/org/apache/cassandra/io/sstable/ColumnNameHelper.java
**Category**: Logic error in condition

---

## Root Cause

`ColumnNameHelper.mergeMin/mergeMax` determined how many components to compare by checking `instanceof CellName` and, if true, calling `clusteringSize()`. Range tombstone bound names written by Cassandra 2.0 are not `CellName` instances, so the fallback `candidate.size()` was used instead — which includes the collection element component. Comparing the collection element component across different cells would crash. The fix replaces both size calculations with `Math.min(candidate.size(), comparator.clusteringPrefixSize())`, which is correct regardless of the concrete name type.

### Before / After

```java
// BEFORE
int size = candidate instanceof CellName
    ? ((CellName)candidate).clusteringSize()
    : candidate.size();
```

```java
// AFTER
int size = Math.min(candidate.size(), comparator.clusteringPrefixSize());
```

---

## How to Spot

When code uses `instanceof` to branch on sub-types and one branch falls back to a less-safe default, check whether all possible runtime types are covered. Here the 2.0 range tombstone bound type slipped through the `instanceof` guard and reached the unsafe fallback.

---

## Test

No test added.
