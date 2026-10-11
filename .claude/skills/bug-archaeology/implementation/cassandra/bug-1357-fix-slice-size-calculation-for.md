# Bug 1357: Wrong Slice Size for Internal Secondary Index Table Queries

**Commit**: 90780b550f · **Ticket**: CASSANDRA-8550
**Subsystem**: src/java/org/apache/cassandra/db/index/composites/CompositesSearcher.java
**Category**: Wrong constant / default value

---

## Root Cause

`CompositesSearcher` sized each page of the internal secondary index table read using `rowsPerQuery`, which was derived by dividing `filter.maxColumns()` by the mean column count of the index CFS. Because each row in a secondary index table corresponds to a single index cell (not a wide row with many columns), dividing by `meanColumns` produced a value far smaller than intended — often just 1 — which disabled the two-row minimum safety net needed to avoid paging regressions when the first result row does not satisfy all filter clauses. The fix replaces the mean-column-based approximation with a direct cap of `min(maxColumns, maxRows)` clamped to at least 2.

### Before / After

```java
// BEFORE — divided by mean column count, producing incorrectly small page size
private int meanColumns = Math.max(index.getIndexCfs().getMeanColumns(), 1);
// We shouldn't fetch only 1 row as this provides buggy paging ...
private int rowsPerQuery = Math.max(Math.min(filter.maxRows(), filter.maxColumns() / meanColumns), 2);
```

```java
// AFTER — direct min of rows/columns limit, always at least 2
// We have to fetch at least two rows to avoid breaking paging if the first row doesn't satisfy all clauses
private int indexCellsPerQuery = Math.max(2, Math.min(filter.maxColumns(), filter.maxRows()));
```

---

## How to Spot

When computing a page size for a special-purpose internal table whose schema differs from the user table, double-check that the divisor reflects the actual cell-per-row ratio of that internal table, not the user table. Reusing `getMeanColumns()` from one CFS to size queries against a different CFS (the index CFS) conflates two different data models. Reviewers should ask: "does the denominator in this page-size formula apply to the table actually being queried?"

---

## Test

No new test added. The fix is validated by the broader secondary index query tests, and the comment in the code explains the two-row minimum invariant required for correct paging.
