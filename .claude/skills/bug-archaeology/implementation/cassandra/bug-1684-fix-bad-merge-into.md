# Bug 1684: Fix Bad Merge of CASSANDRA-6825 Into cassandra-2.1

**Commit**: 3a51a7e83a · **Ticket**: CASSANDRA-6978
**Subsystem**: src/java/org/apache/cassandra/db/filter
**Category**: Logic error in condition

---

## Root Cause

When CASSANDRA-6825 was merged into the cassandra-2.1 branch, the `ColumnSlice.intersects()` method received a broken guard condition. The original intent was to skip the min/max check for the first composite component (which is handled separately) and to guard each subsequent check against an out-of-bounds access when the start or end composite has fewer components than the index `i`. The merge produced a single condition that applied the bounds of `sEnd` to the `sStart` comparison and vice versa, causing incorrect `false` returns — falsely reporting no intersection — when one side of the slice was a prefix composite (had fewer components than the SSTable min/max cell names).

### Before / After

```java
// BEFORE (bad merge)
if (i > 0 && (t.compare(f, minCellNames.get(i)) < 0 ||
              t.compare(s, maxCellNames.get(i)) > 0))
    return false;
```

```java
// AFTER
if (i > 0 && (i < sEnd.size() && t.compare(f, minCellNames.get(i)) < 0 ||
              i < sStart.size() && t.compare(s, maxCellNames.get(i)) > 0))
    return false;
```

---

## How to Spot

When porting a fix across branches with different composite-key APIs, each comparison that indexes into a list of components must be guarded by a bounds check against the correct list — `sEnd` guards the end-component access, `sStart` guards the start-component access. Reviewing merged conditions for cross-list index/bounds mismatches catches this class of error.

---

## Test

`ColumnSliceTest` was extended with a case for a slice whose start and end are both single-component composites (e.g., `composite(1)` to `composite(1)`) intersecting an SSTable with a three-component min/max range; this previously returned `false` incorrectly.
