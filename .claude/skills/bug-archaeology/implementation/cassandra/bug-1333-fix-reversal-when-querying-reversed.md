# Bug 1333: Pager Clones ColumnFamily With Wrong Sort Order When Discarding Rows From Reversed Slices

**Commit**: 58e6a1d8d1 · **Ticket**: CASSANDRA-8767
**Subsystem**: src/java/org/apache/cassandra/service/pager/AbstractQueryPager.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`AbstractQueryPager` needed to create a shallow copy of a `ColumnFamily` to hold the subset of columns remaining after discarding rows at page boundaries. The zero-argument `cloneMeShallow()` always forwarded the current CF's own `isInsertReversed()` flag as the sort order of the clone. When a query was issued with reversed ordering (`ORDER BY ... DESC`), the discard operations (`discardFirst`/`discardLast`) operated on a clone that was also marked reversed, causing them to remove the wrong end of the column set and producing an `AssertionError` or incorrect results. The fix adds a `cloneMeShallow(boolean reversed)` overload so the pager can explicitly pass `isReversed()` from query state rather than inheriting it from the CF's internal flag.

### Before / After

```java
// BEFORE
firstCf = first.cf.cloneMeShallow();  // always uses CF's own isInsertReversed()
```

```java
// AFTER
firstCf = first.cf.cloneMeShallow(isReversed());  // uses query-level reversal flag
```

---

## How to Spot

When a method copies or projects a stateful object and the copy will be used in a different context (e.g. page-boundary trimming vs. normal insertion), check whether the copy inherits flags that are only meaningful in the source context. In particular, any clone that is used for column-discard arithmetic after a reversal reorder should derive its ordering flag from the query context, not from the CF's own storage-level flag.

---

## Test

No test added (the fix prevents an `AssertionError` triggered by DESC ordering with paging; a regression test was not included in this commit).
