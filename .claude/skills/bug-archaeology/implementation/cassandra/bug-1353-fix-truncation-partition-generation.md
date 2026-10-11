# Bug 1353: cassandra-stress User-Mode Truncation of Partition Generation

**Commit**: 1435b9a87a · **Ticket**: CASSANDRA-8608
**Subsystem**: tools/stress/src/org/apache/cassandra/stress/generate/PartitionIterator.java
**Category**: Logic error in condition

---

## Root Cause

`PartitionIterator.MultiRowIterator` used a field named `limit` to track the last row that the iterator was permitted to visit. The `compareToLastRow` method compared the current iterator position against this limit, but the off-by-one relationship between the scalar position and the decomposed per-level limit was handled incorrectly: when `setLimit(position)` decomposed a seed's prior position into per-level counts, the resulting `expectedRowCount` did not include the row at `position` itself, causing reads to stop one row short. Additionally, the `reset` method was passed the full `Operation` object solely to call `op.isWrite()`, creating an unnecessary coupling; the refactor replaced it with a plain `boolean isWrite` parameter, which exposed the real flow more clearly and allowed the write-path's position arithmetic to be corrected.

### Before / After

```java
// BEFORE — limit decomposition excluded the target row; compareToLastRow had wrong off-by-one
else if (position != 0)
    expectedRowCount = setLimit(position);   // position not +1; last row omitted

private int setLimit(int position)
{
    decompose(position, limit);
    int expectedRowCount = 0;
    for (int i = 0 ; i < limit.length ; i++)
        expectedRowCount += limit[i] * generator.clusteringDescendantAverages[i];
    return expectedRowCount;          // does not count the row at 'position'
}
```

```java
// AFTER — last row set to position-1 of current seed, expectedRowCount includes +1
else if (position != 0)
    expectedRowCount = setLastRow(position - 1);

private int setLastRow(int position)
{
    decompose(position, lastRow);
    int expectedRowCount = 0;
    for (int i = 0 ; i < lastRow.length ; i++)
        expectedRowCount += lastRow[i] * generator.clusteringDescendantAverages[i];
    return expectedRowCount + 1;      // +1 accounts for the row at 'position'
}
```

---

## How to Spot

When a method decomposes a scalar position into per-level counts for a hierarchical iterator and the result is used both as a limit and as an expected row count, verify the off-by-one: is the row *at* the requested position included or excluded? Reviewers should also be alert to functions that accept a large object (`Operation`) purely to extract a single boolean — the unnecessary coupling can hide that the boolean is computed too late (after state mutation) or used inconsistently across call paths.

---

## Test

No new test added. The regression was observable by running cassandra-stress user mode and verifying that reads visited the correct number of rows.
