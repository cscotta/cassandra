# Bug 1690: Fix Bad Skip of SSTables on Slice Query with Composite Keys

**Commit**: b218536dbc · **Ticket**: CASSANDRA-6825
**Subsystem**: src/java/org/apache/cassandra/db/marshal/CompositeType.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`CompositeType.intersects()` determined whether a slice query overlaps with the min/max column range stored in an SSTable. The original logic iterated over each component independently and required every component to intersect—treating a composite key as a conjunction of per-component ranges. This is wrong: if the first component of the slice end is *equal* to the first component of the SSTable min, but the second component falls *before* the SSTable min's second component, the slice still does not intersect. The old code incorrectly returned `true` in that case, causing later skipping to also fail. The fix rewrites the check to first do a proper composite-key comparison (`compare()`) of the full slice bounds against the SSTable bounds, and then applies the per-component optimization only when the first component is pinned to a single value.

### Before / After

```java
// BEFORE – per-component independent intersection, wrong for multi-component boundaries
for (int i = 0; i < minColumnNames.size(); i++) {
    AbstractType<?> t = types.get(i);
    ByteBuffer s = i < start.length ? start[i] : ByteBufferUtil.EMPTY_BYTE_BUFFER;
    ByteBuffer f = i < finish.length ? finish[i] : ByteBufferUtil.EMPTY_BYTE_BUFFER;
    if (!t.intersects(minColumnNames.get(i), maxColumnNames.get(i), s, f))
        continue outer;
}
return true;
```

```java
// AFTER – full composite comparison first, then per-component refinement
if (compare(start, maxColumnNames, true) > 0 || compare(finish, minColumnNames, false) < 0)
    continue;  // slice does not intersect

for (int i = 0; i < Math.min(...); i++) {
    if (i > 0 && !t.intersects(minColumnNames.get(i), maxColumnNames.get(i), start[i], finish[i]))
        continue outer;
    if (t.compare(start[i], finish[i]) != 0)
        break;
}
return true;
```

---

## How to Spot

Intersection tests on composite (multi-component) keys must treat the components as a lexicographic tuple, not as independent ranges. Checking each component in isolation gives false positives when a boundary component matches but a later component excludes the range.

---

## Test

`CompositeTypeTest.testIntersectsSingleSlice()` and `testIntersectsMultipleSlices()` were added, covering dozens of boundary conditions including partial component matches, reversed slices, and missing components.
