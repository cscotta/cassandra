# Bug 1563: Fix Error When Doing Reverse Queries with Static Columns

**Commit**: 6893130e89 · **Ticket**: CASSANDRA-7490
**Subsystem**: cql3 / statements / SelectStatement
**Category**: Logic error in condition

---

## Root Cause

When building a column slice filter for a query that involves both a static-column slice and a clustering-column slice, the code always tested whether the two slices overlapped using the forward comparator (`cfDef.cfm.comparator`). For reversed queries the clustering slice has its start and finish swapped relative to the forward order, so the overlap test produced the wrong answer and the slices were assembled in the wrong order (or with the wrong boundary). This led to incorrect or empty results for `ORDER BY c DESC` queries on tables with static columns.

### Before / After

```java
// BEFORE – always uses forward comparator even when isReversed
return staticSlice == null
     ? sliceFilter(slice, limit, toGroup)
     : (slice.includes(cfDef.cfm.comparator, staticSlice.finish)
            ? sliceFilter(new ColumnSlice(staticSlice.start, slice.finish), limit, toGroup)
            : sliceFilter(new ColumnSlice[]{ staticSlice, slice }, limit, toGroup));
```

```java
// AFTER – branches on isReversed and uses reverseComparator when needed
if (staticSlice == null)
    return sliceFilter(slice, limit, toGroup);

if (isReversed)
    return slice.includes(cfDef.cfm.comparator.reverseComparator, staticSlice.start)
            ? sliceFilter(new ColumnSlice(slice.start, staticSlice.finish), limit, toGroup)
            : sliceFilter(new ColumnSlice[]{ slice, staticSlice }, limit, toGroup);
else
    return slice.includes(cfDef.cfm.comparator, staticSlice.finish)
            ? sliceFilter(new ColumnSlice(staticSlice.start, slice.finish), limit, toGroup)
            : sliceFilter(new ColumnSlice[]{ staticSlice, slice }, limit, toGroup);
```

---

## How to Spot

Any code that computes a range or slice ordering should explicitly check whether the query is reversed before choosing which comparator to apply. Reusing a forward comparator for reverse-order logic is a category of error to flag in code review whenever `isReversed` is in scope.

---

## Test

A new test class `SliceQueryFilterWithStaticColumnsPresentTest` was added covering all combinations of default ordering, `ORDER BY c ASC`, and `ORDER BY c DESC` against both a normal table and a `CLUSTERING ORDER BY (c DESC)` table, with point, range, and `IN` predicates.
